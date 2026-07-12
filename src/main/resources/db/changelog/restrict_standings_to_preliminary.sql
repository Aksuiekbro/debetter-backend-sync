--liquibase formatted sql

--changeset result-integrity:restrict-standings-to-preliminary splitStatements:false
--comment: Match-local scores, winners, and completion persist for every stage. Cumulative team.preliminary_score and tournament_participant.speaker_score change only for PRELIMINARY matches.
CREATE OR REPLACE FUNCTION update_match_scores_bulk(results_json jsonb)
RETURNS void AS $$
BEGIN
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

    WITH raw_team_scores AS (
        SELECT m.team1_id AS team_id, i.team1score AS delta
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.team1score IS NOT NULL
        UNION ALL
        SELECT m.team2_id, i.team2score
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.team2score IS NOT NULL
        UNION ALL
        SELECT m.team3_id, i.team3score
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.team3score IS NOT NULL
        UNION ALL
        SELECT m.team4_id, i.team4score
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.team4score IS NOT NULL
    ), team_scores AS (
        SELECT team_id, SUM(delta) AS delta
        FROM raw_team_scores
        WHERE team_id IS NOT NULL
        GROUP BY team_id
    )
    UPDATE team t
    SET preliminary_score = COALESCE(t.preliminary_score, 0) + ts.delta
    FROM team_scores ts
    WHERE t.id = ts.team_id;

    WITH raw_debater_scores AS (
        SELECT m.debater1_id AS participant_id, i.debater1score AS delta
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.debater1score IS NOT NULL
        UNION ALL
        SELECT m.debater2_id, i.debater2score
        FROM _bulk_input i
        JOIN "match" m ON m.id = i.match_id
        JOIN round r ON r.id = m.round_id
        JOIN round_group rg ON rg.id = r.round_group_id
        WHERE rg.type = 'PRELIMINARY' AND rg.tournament_id = i.tournament_id AND i.debater2score IS NOT NULL
    ), debater_scores AS (
        SELECT participant_id, SUM(delta) AS delta
        FROM raw_debater_scores
        WHERE participant_id IS NOT NULL
        GROUP BY participant_id
    )
    UPDATE tournament_participant tp
    SET speaker_score = COALESCE(tp.speaker_score, 0) + ds.delta
    FROM debater_scores ds
    WHERE tp.id = ds.participant_id;
END;
$$ LANGUAGE plpgsql;
