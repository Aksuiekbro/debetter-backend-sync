CREATE OR REPLACE FUNCTION update_match_scores_bulk(results_json jsonb)
RETURNS void AS $$
BEGIN
    WITH input AS (
        SELECT *
        FROM jsonb_to_recordset(results_json) AS x(
            tournament_id BIGINT,
            match_id BIGINT,
            team1score INTEGER,
            team2score INTEGER,
            team3score INTEGER,
            team4score INTEGER,
            debater1score INTEGER,
            debater2score INTEGER
        )
    )

    UPDATE "match" m
    SET team1score    = COALESCE(i.team1score, m.team1score),
        team2score    = COALESCE(i.team2score, m.team2score),
        team3score    = COALESCE(i.team3score, m.team3score),
        team4score    = COALESCE(i.team4score, m.team4score),
        debater1score = COALESCE(i.debater1score, m.debater1score),
        debater2score = COALESCE(i.debater2score, m.debater2score),
        completed     = true
    FROM input i
    JOIN round r ON m.round_id = r.id
    JOIN round_group rg ON r.round_group_id = rg.id
    JOIN tournament t ON rg.tournament_id = t.id
    WHERE m.id = i.match_id
      AND t.id = i.tournament_id;

    WITH team_scores AS (
        SELECT (m.team1_id) AS team_id, SUM(i.team1score) AS delta
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.team1score IS NOT NULL
        GROUP BY m.team1_id
        UNION ALL
        SELECT m.team2_id, SUM(i.team2score)
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.team2score IS NOT NULL
        GROUP BY m.team2_id
        UNION ALL
        SELECT m.team3_id, SUM(i.team3score)
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.team3score IS NOT NULL
        GROUP BY m.team3_id
        UNION ALL
        SELECT m.team4_id, SUM(i.team4score)
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.team4score IS NOT NULL
        GROUP BY m.team4_id
    )
    UPDATE team t
    SET preliminary_score = t.preliminary_score + ts.delta
    FROM team_scores ts
    WHERE t.id = ts.team_id;

    WITH debater_scores AS (
        SELECT m.debater1_id AS debater_id, SUM(i.debater1score) AS delta
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.debater1score IS NOT NULL
        GROUP BY m.debater1_id
        UNION ALL
        SELECT m.debater2_id, SUM(i.debater2score)
        FROM input i
        JOIN match m ON m.id = i.match_id
        WHERE i.debater2score IS NOT NULL
        GROUP BY m.debater2_id
    )
    UPDATE debater d
    SET speaker_score = d.speaker_score + ds.delta
    FROM debater_scores ds
    WHERE d.id = ds.debater_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION assign_judges_for_round(p_round_id BIGINT)
RETURNS void AS $$
DECLARE
    v_tournament_id BIGINT;
    v_judge_count INT;
BEGIN
    SELECT rg.tournament_id
    INTO v_tournament_id
    FROM round r
    JOIN round_group rg
      ON r.round_group_id = rg.id
    WHERE r.id = p_round_id;

    SELECT COUNT(*) INTO v_judge_count FROM judge WHERE tournament_id = v_tournament_id AND checked_in = true;

    IF v_judge_count = 0 THEN
        RETURN;
    END IF;

    WITH RankedJudges AS (
        SELECT id, ROW_NUMBER() OVER (ORDER BY times_judged ASC, id ASC) as rn
        FROM judge
        WHERE tournament_id = v_tournament_id AND checked_in = true
    ),
    UnassignedMatches AS (
        SELECT id, ROW_NUMBER() OVER (ORDER BY id ASC) as rn
        FROM match
        WHERE round_id = p_round_id AND judge_id IS NULL
    )
    UPDATE match
    SET judge_id = rj.id
    FROM UnassignedMatches um
    JOIN RankedJudges rj ON (um.rn - 1) % v_judge_count = (rj.rn - 1)
    WHERE match.id = um.id;

    WITH NewAssignments AS (
        SELECT judge_id, COUNT(id) as num_assigned
        FROM match
        WHERE round_id = p_round_id
        GROUP BY judge_id
    )
    UPDATE judge
    SET times_judged = judge.times_judged + na.num_assigned
    FROM NewAssignments na
    WHERE judge.id = na.judge_id;

END;
$$ LANGUAGE plpgsql;