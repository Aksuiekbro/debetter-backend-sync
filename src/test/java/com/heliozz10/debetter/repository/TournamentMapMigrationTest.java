package com.heliozz10.debetter.repository;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentMapMigrationTest {
    @Test
    void migrationCreatesTheExpectedMapSchemaAndConstraints() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:tournament_map_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tournament (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE url (id BIGINT PRIMARY KEY)");
        }

        new ResourceDatabasePopulator(
                new ClassPathResource("db/changelog/create_tournament_map.sql")
        ).execute(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertColumn(statement, "TITLE", 120L, "NO");
            assertColumn(statement, "DESCRIPTION", 5000L, "NO");
            assertEquals(50L, queryLong(
                    statement,
                    "SELECT INCREMENT FROM INFORMATION_SCHEMA.SEQUENCES "
                            + "WHERE SEQUENCE_NAME = 'TOURNAMENT_MAP_SEQ'"
            ));

            statement.execute("INSERT INTO tournament (id) VALUES (1), (2)");
            statement.execute("INSERT INTO url (id) VALUES (10), (11)");
            statement.execute("""
                    INSERT INTO tournament_map (id, title, description, tournament_id, image_id)
                    VALUES (NEXT VALUE FOR tournament_map_seq, 'Venue', 'Directions', 1, 10)
                    """);

            assertThrows(SQLException.class, () -> statement.execute("""
                    INSERT INTO tournament_map (id, title, description, tournament_id, image_id)
                    VALUES (NEXT VALUE FOR tournament_map_seq, 'Duplicate tournament', 'Directions', 1, 11)
                    """));
            assertThrows(SQLException.class, () -> statement.execute("""
                    INSERT INTO tournament_map (id, title, description, tournament_id, image_id)
                    VALUES (NEXT VALUE FOR tournament_map_seq, 'Duplicate image', 'Directions', 2, 10)
                    """));

            statement.execute("DELETE FROM tournament WHERE id = 1");
            assertEquals(0L, queryLong(statement, "SELECT COUNT(*) FROM tournament_map"));
        }
    }

    private static void assertColumn(
            Statement statement,
            String columnName,
            long expectedLength,
            String expectedNullable
    ) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'TOURNAMENT_MAP' AND COLUMN_NAME = '%s'
                """.formatted(columnName))) {
            result.next();
            assertEquals(expectedLength, result.getLong("CHARACTER_MAXIMUM_LENGTH"));
            assertEquals(expectedNullable, result.getString("IS_NULLABLE"));
        }
    }

    private static long queryLong(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
