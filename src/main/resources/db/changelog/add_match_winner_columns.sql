--liquibase formatted sql

--changeset winner-contract:add-match-winner-columns
--comment: Per-team explicit winner (the organizer/judge's Win/Loss pick). APF stores 1 won / 1 lost; BPF stores 2 won / 2 lost (requirements Q19, Q20). Idempotent so it no-ops if the columns already exist (Hibernate ddl-auto=update or a partial sync). No underscore, matching the existing team{n}score columns.
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team1won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team2won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team3won BOOLEAN;
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS team4won BOOLEAN;

--changeset winner-contract:fix-and-extend-update-match-scores-bulk runOnChange:true splitStatements:false
--comment: Rewrite update_match_scores_bulk to (1) FIX two pre-existing bugs that made it throw and roll back every submit, and (2) persist team winners. Bug 1: the UPDATE referenced the target alias "m" inside a FROM join ("invalid reference to FROM-clause entry for table m"); fixed via a correlated subquery for the tournament-ownership check. Bug 2: the standings CTEs referenced the first statement's "input" CTE, which is out of scope; fixed by materialising the payload into a temp table all statements can read. Existing team/debater score aggregation is PRESERVED unchanged (so team-elimination seeding by preliminary_score keeps working). NOTE: the +=delta aggregation double-counts if results are edited (Q45), and team standings should ultimately be win-based per requirement #33 — both are intentionally left for the separate standings/LD-selection redesign, NOT changed here. Carried as a new changeset (not by editing the checksummed create_functions.sql include) to avoid Liquibase checksum failures on deployed DBs.
CREATE OR REPLACE FUNCTION update_match_scores_bulk(results_json jsonb)
RETURNS void AS $$
BEGIN
    -- Materialise the payload so every statement can read it (the previous version's
    -- "input" CTE was scoped to the first statement only, which made statements 2/3 throw).
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

    -- Persist match scores + explicit winners + completed.
    -- Ownership check uses a correlated subquery, NOT a FROM-join on the target alias
    -- (referencing target "m" inside a FROM join is invalid in PostgreSQL and was the bug).
    UPDATE "match" m
    SET team1score    = COALESCE(i.team1score, m.team1score),
        team2score    = COALESCE(i.team2score, m.team2score),
        team3score    = COALESCE(i.team3score, m.team3score),
        team4score    = COALESCE(i.team4score, m.team4score),
        team1won      = COALESCE(i.team1won, m.team1won),
        team2won      = COALESCE(i.team2won, m.team2won),
        team3won      = COALESCE(i.team3won, m.team3won),
        team4won      = COALESCE(i.team4won, m.team4won),
        debater1score = COALESCE(i.debater1score, m.debater1score),
        debater2score = COALESCE(i.debater2score, m.debater2score),
        completed     = true
    FROM _bulk_input i
    WHERE m.id = i.match_id
      AND m.round_id IN (
          SELECT r.id
          FROM round r
          JOIN round_group rg ON r.round_group_id = rg.id
          WHERE rg.tournament_id = i.tournament_id
      );

    -- Team standings accumulation (preserved unchanged from the original function;
    -- team-elimination seeding in RoundGroupService sorts by preliminary_score).
    WITH team_scores AS (
        SELECT m.team1_id AS team_id, SUM(i.team1score) AS delta
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.team1score IS NOT NULL
        GROUP BY m.team1_id
        UNION ALL
        SELECT m.team2_id, SUM(i.team2score)
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.team2score IS NOT NULL
        GROUP BY m.team2_id
        UNION ALL
        SELECT m.team3_id, SUM(i.team3score)
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.team3score IS NOT NULL
        GROUP BY m.team3_id
        UNION ALL
        SELECT m.team4_id, SUM(i.team4score)
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.team4score IS NOT NULL
        GROUP BY m.team4_id
    )
    UPDATE team t
    SET preliminary_score = t.preliminary_score + ts.delta
    FROM team_scores ts
    WHERE t.id = ts.team_id;

    -- Debater speaker-score accumulation (preserved unchanged).
    WITH debater_scores AS (
        SELECT m.debater1_id AS debater_id, SUM(i.debater1score) AS delta
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.debater1score IS NOT NULL
        GROUP BY m.debater1_id
        UNION ALL
        SELECT m.debater2_id, SUM(i.debater2score)
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        WHERE i.debater2score IS NOT NULL
        GROUP BY m.debater2_id
    )
    UPDATE debater d
    SET speaker_score = d.speaker_score + ds.delta
    FROM debater_scores ds
    WHERE d.id = ds.debater_id;
END;
$$ LANGUAGE plpgsql;
