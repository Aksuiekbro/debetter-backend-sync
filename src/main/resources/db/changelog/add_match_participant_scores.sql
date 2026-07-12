--liquibase formatted sql

--changeset participant-score-contract:add-match-participant-scores
--comment: Store each team-format speaker result per match. Historic completed team matches intentionally have no rows and remain identifiable for repair; totals in match.team{n}_score are not substituted for speaker scores.
CREATE SEQUENCE IF NOT EXISTS match_participant_score_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS match_participant_score (
    id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0),
    CONSTRAINT match_participant_score_pkey PRIMARY KEY (id),
    CONSTRAINT match_participant_score_match_participant_key UNIQUE (match_id, participant_id),
    CONSTRAINT match_participant_score_match_fkey FOREIGN KEY (match_id) REFERENCES "match" (id),
    CONSTRAINT match_participant_score_participant_fkey FOREIGN KEY (participant_id) REFERENCES tournament_participant (id)
);

CREATE INDEX IF NOT EXISTS match_participant_score_match_id_idx ON match_participant_score (match_id);
CREATE INDEX IF NOT EXISTS match_participant_score_participant_id_idx ON match_participant_score (participant_id);
