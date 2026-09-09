--liquibase formatted sql

--changeset account-profile:create-persistent-logins
--comment: Persist explicit remember-me opt-ins using Spring Security's JDBC token schema.
CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS persistent_logins_username_idx ON persistent_logins (username);
