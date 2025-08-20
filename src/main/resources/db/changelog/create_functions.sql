CREATE OR REPLACE FUNCTION update_match_scores_bulk(results_json jsonb)
RETURNS void AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
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
    LOOP
        UPDATE match
        SET team1score    = rec.team1score,
            team2score    = rec.team2score,
            team3score    = rec.team3score,
            team4score    = rec.team4score,
            debater1score = rec.debater1score,
            debater2score = rec.debater2score,
            completed     = true
        WHERE id = rec.match_id
        AND tournament_id = rec.tournament_id;

        UPDATE team
        SET preliminary_score = preliminary_score + rec.team1score
        WHERE id = (SELECT team1_id FROM match WHERE id = rec.match_id);

        UPDATE team
        SET preliminary_score = preliminary_score + rec.team2score
        WHERE id = (SELECT team2_id FROM match WHERE id = rec.match_id);

        UPDATE team
        SET preliminary_score = preliminary_score + rec.team3score
        WHERE id = (SELECT team3_id FROM match WHERE id = rec.match_id);

        UPDATE team
        SET preliminary_score = preliminary_score + rec.team4score
        WHERE id = (SELECT team4_id FROM match WHERE id = rec.match_id);

        UPDATE debater
        SET speaker_score = speaker_score + rec.debater1score
        WHERE id = (SELECT debater1_id FROM match WHERE id = rec.match_id);

        UPDATE debater
        SET speaker_score = speaker_score + rec.debater2score
        WHERE id = (SELECT debater2_id FROM match WHERE id = rec.match_id);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION assign_judges_for_round(p_round_id BIGINT)
RETURNS void AS $$
DECLARE
    v_tournament_id BIGINT;
    v_judge_count INT;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM round WHERE id = p_round_id;

    SELECT COUNT(*) INTO v_judge_count FROM judge WHERE tournament_id = v_tournament_id;

    IF v_judge_count = 0 THEN
        RETURN;
    END IF;

    WITH RankedJudges AS (
        SELECT id, ROW_NUMBER() OVER (ORDER BY times_judged ASC, id ASC) as rn
        FROM judge
        WHERE tournament_id = v_tournament_id
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