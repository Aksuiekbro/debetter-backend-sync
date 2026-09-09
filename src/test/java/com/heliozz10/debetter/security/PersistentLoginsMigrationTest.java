package com.heliozz10.debetter.security;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class PersistentLoginsMigrationTest {
    @Test
    void postgresCompatibleMigrationPreservesExistingRowsAndSupportsTheRealJdbcRepository() {
        JdbcDataSource database = new JdbcDataSource();
        database.setURL("jdbc:h2:mem:account_token_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new ClassPathResource("db/changelog/create_persistent_logins.sql"));
        migration.execute(database);
        JdbcTokenRepositoryImpl tokens = new JdbcTokenRepositoryImpl();
        tokens.setDataSource(database);
        tokens.createNewToken(new PersistentRememberMeToken("testuser", "test-series", "first-token", new Date()));
        migration.execute(database);
        assertEquals("testuser", tokens.getTokenForSeries("test-series").getUsername());
        tokens.updateToken("test-series", "second-token", new Date());
        assertEquals("second-token", tokens.getTokenForSeries("test-series").getTokenValue());
        tokens.removeUserTokens("testuser");
        assertNull(tokens.getTokenForSeries("test-series"));
    }
}
