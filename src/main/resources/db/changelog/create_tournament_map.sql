--liquibase formatted sql

--changeset tournament-map:create-tournament-map
--comment: Store one title, description, and image-backed map for each tournament.
CREATE SEQUENCE IF NOT EXISTS tournament_map_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS tournament_map (
    id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    tournament_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    CONSTRAINT tournament_map_pkey PRIMARY KEY (id),
    CONSTRAINT tournament_map_tournament_key UNIQUE (tournament_id),
    CONSTRAINT tournament_map_image_key UNIQUE (image_id),
    CONSTRAINT tournament_map_tournament_fkey
        FOREIGN KEY (tournament_id) REFERENCES tournament (id) ON DELETE CASCADE,
    CONSTRAINT tournament_map_image_fkey
        FOREIGN KEY (image_id) REFERENCES url (id)
);
