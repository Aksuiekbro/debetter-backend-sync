--liquibase formatted sql

--changeset winner-contract:add-match-winner-columns
--comment: Per-team explicit winner (the organizer/judge's Win/Loss pick). APF stores 1 won / 1 lost; BPF stores 2 won / 2 lost (requirements Q19, Q20). Physical columns are team{n}_won (UNDERSCORE) to match the deployed Hibernate schema, where the entity Match.team1Score maps to physical column team1_score. Idempotent (ADD COLUMN IF NOT EXISTS).
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team1_won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team2_won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team3_won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team4_won BOOLEAN;

--changeset winner-contract:fix-and-extend-update-match-scores-bulk runOnChange:true splitStatements:false
--comment: Rewrite update_match_scores_bulk against the ACTUAL deployed schema. The previous repo function (create_functions.sql) was badly out of sync with production - it referenced m.team1score (no underscore) but the Hibernate columns are team1_score; it UPDATE'd a non-existent "debater" table (participants are tournament_participant); and it had two SQL bugs (the UPDATE target alias referenced inside a FROM join, and an out-of-scope "input" CTE in the standings statements). So the function threw on every submit and results never persisted. This version: uses the real physical columns (team{n}_score, team{n}_won, team{n}_id, debater{n}_score, debater{n}_id, tournament_participant.speaker_score, team.preliminary_score, round.round_group_id, round_group.tournament_id); maps the JSON/recordset keys the service builds (team{n}score, team{n}won, debater{n}score - no underscore) onto those columns; fixes both SQL bugs via a correlated ownership subquery and a shared temp table; and persists the per-team winner. Validated against the production PostgreSQL on a real match. Carried in a new changeset (not by editing the checksummed create_functions.sql) so deployed DBs do not fail checksum validation; runOnChange re-applies it if edited.
CREATE OR REPLACE FUNCTION update_match_scores_bulk(results_json jsonb)
RETURNS void AS $$
BEGIN
    -- Shared payload so every statement can read it (the old "input" CTE was scoped to the first statement only).
    DROP TABLE IF EXISTS _bulk_input;
    CREATE TEMP TABLE _bulk_input ON COMMIT DROP AS
    SELECT *
    FROM jsonb_to_recordset(results_json) AS x(
        tournament_id BIGINT,
        match_id BIGINT,
        team1score INTEGER,
        team2score INTEGER,
        team3score INTEGER,
        team4score INTEGER,
        team1won BOOLEAN,
        team2won BOOLEAN,
        team3won BOOLEAN,
        team4won BOOLEAN,
        debater1score INTEGER,
        debater2score INTEGER
    );

    -- Persist scores + explicit winners + completed.
    -- Physical DB columns are underscore (team1_score, team1_won); recordset/JSON keys are not (i.team1score, i.team1won).
    -- Ownership via a correlated subquery (referencing the UPDATE target inside a FROM join is invalid in PostgreSQL).
    UPDATE "match" m
    SET team1_score    = COALESCE(i.team1score, m.team1_score),
        team2_score    = COALESCE(i.team2score, m.team2_score),
        team3_score    = COALESCE(i.team3score, m.team3_score),
        team4_score    = COALESCE(i.team4score, m.team4_score),
        team1_won      = COALESCE(i.team1won, m.team1_won),
        team2_won      = COALESCE(i.team2won, m.team2_won),
        team3_won      = COALESCE(i.team3won, m.team3_won),
        team4_won      = COALESCE(i.team4won, m.team4_won),
        debater1_score = COALESCE(i.debater1score, m.debater1_score),
        debater2_score = COALESCE(i.debater2score, m.debater2_score),
        completed      = true
    FROM _bulk_input i
    WHERE m.id = i.match_id
      AND m.round_id IN (
          SELECT r.id
          FROM round r
          JOIN round_group rg ON r.round_group_id = rg.id
          WHERE rg.tournament_id = i.tournament_id
      );

    -- Team standings accumulation (preserved; team-elimination seeding sorts by preliminary_score).
    WITH team_scores AS (
        SELECT m.team1_id AS team_id, SUM(i.team1score) AS delta
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.team1score IS NOT NULL GROUP BY m.team1_id
        UNION ALL
        SELECT m.team2_id, SUM(i.team2score)
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.team2score IS NOT NULL GROUP BY m.team2_id
        UNION ALL
        SELECT m.team3_id, SUM(i.team3score)
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.team3score IS NOT NULL GROUP BY m.team3_id
        UNION ALL
        SELECT m.team4_id, SUM(i.team4score)
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.team4score IS NOT NULL GROUP BY m.team4_id
    )
    UPDATE team t
    SET preliminary_score = COALESCE(t.preliminary_score, 0) + ts.delta
    FROM team_scores ts
    WHERE t.id = ts.team_id;

    -- Speaker-point accumulation per participant (the match's debater slots are tournament_participant rows).
    WITH debater_scores AS (
        SELECT m.debater1_id AS participant_id, SUM(i.debater1score) AS delta
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.debater1score IS NOT NULL GROUP BY m.debater1_id
        UNION ALL
        SELECT m.debater2_id, SUM(i.debater2score)
        FROM _bulk_input i JOIN "match" m ON m.id = i.match_id
        WHERE i.debater2score IS NOT NULL GROUP BY m.debater2_id
    )
    UPDATE tournament_participant tp
    SET speaker_score = COALESCE(tp.speaker_score, 0) + ds.delta
    FROM debater_scores ds
    WHERE tp.id = ds.participant_id;
END;
$$ LANGUAGE plpgsql;
