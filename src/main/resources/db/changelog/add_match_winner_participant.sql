--liquibase formatted sql

--changeset outcome-only-elimination:add-match-winner-participant
--comment: Persist the explicit winner for SOLO_ELIMINATION without storing synthetic speaker points.
ALTER TABLE "match" ADD COLUMN IF NOT EXISTS winner_participant_id BIGINT;

--changeset outcome-only-elimination:backfill-match-winner-participant
--comment: Preserve progression for completed legacy SOLO_ELIMINATION matches that stored the winner through distinct debater scores.
UPDATE "match" m
SET winner_participant_id = CASE
    WHEN m.debater1_score > m.debater2_score THEN m.debater1_id
    ELSE m.debater2_id
END
FROM round r
JOIN round_group rg ON rg.id = r.round_group_id
WHERE m.round_id = r.id
  AND rg.type = 'SOLO_ELIMINATION'
  AND m.completed IS TRUE
  AND m.winner_participant_id IS NULL
  AND m.debater1_id IS NOT NULL
  AND m.debater2_id IS NOT NULL
  AND m.debater1_score IS NOT NULL
  AND m.debater2_score IS NOT NULL
  AND m.debater1_score <> m.debater2_score;

--changeset outcome-only-elimination:add-match-winner-participant-fk
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = current_schema() AND table_name = 'match' AND constraint_name = 'match_winner_participant_id_fkey'
ALTER TABLE "match"
    ADD CONSTRAINT match_winner_participant_id_fkey
    FOREIGN KEY (winner_participant_id)
    REFERENCES tournament_participant(id)
    ON DELETE SET NULL;
