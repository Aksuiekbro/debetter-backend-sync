package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.TournamentMap;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.tournament.TournamentMapRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/map-postgresql"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TournamentMapPostgresMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.3-alpine3.20");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TournamentMapRepository tournamentMapRepository;

    @BeforeAll
    void applyMapMigrationFromTheFullLiquibaseMaster() throws LiquibaseException {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tournament_map CASCADE");
        jdbcTemplate.execute("DROP SEQUENCE IF EXISTS tournament_map_seq");

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yml");
        liquibase.setAnalyticsEnabled(false);
        liquibase.setShouldRun(true);
        liquibase.afterPropertiesSet();
    }

    @Test
    void migrationCreatesTheProductionSchemaAndRepositoryCanUseIt() {
        assertColumn("title", 120, "NO");
        assertColumn("description", 5000, "NO");
        assertConstraint("tournament_map_pkey", "p");
        assertConstraint("tournament_map_tournament_key", "u");
        assertConstraint("tournament_map_image_key", "u");
        assertConstraint("tournament_map_tournament_fkey", "f");
        assertConstraint("tournament_map_image_fkey", "f");
        assertEquals("c", jdbcTemplate.queryForObject(
                "SELECT confdeltype::text FROM pg_constraint WHERE conname = ?",
                String.class,
                "tournament_map_tournament_fkey"
        ));
        assertEquals(50L, jdbcTemplate.queryForObject(
                "SELECT increment_by FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = ?",
                Long.class,
                "tournament_map_seq"
        ));

        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        TournamentMap saved = tournamentMapRepository.saveAndFlush(tournamentMap(tournament, "first.png"));

        assertEquals("Venue map", tournamentMapRepository.findByTournamentId(tournament.getId())
                .orElseThrow()
                .getTitle());
        assertThrows(
                DataIntegrityViolationException.class,
                () -> tournamentMapRepository.saveAndFlush(tournamentMap(tournament, "second.png"))
        );
        assertEquals(saved.getTournament().getId(), tournament.getId());
    }

    private void assertColumn(String columnName, int expectedLength, String expectedNullable) {
        assertEquals(expectedLength, jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'tournament_map'
                  AND column_name = ?
                """, Integer.class, columnName));
        assertEquals(expectedNullable, jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'tournament_map'
                  AND column_name = ?
                """, String.class, columnName));
    }

    private void assertConstraint(String constraintName, String expectedType) {
        assertEquals(expectedType, jdbcTemplate.queryForObject(
                "SELECT contype::text FROM pg_constraint WHERE conname = ?",
                String.class,
                constraintName
        ));
    }

    private static TournamentMap tournamentMap(Tournament tournament, String filename) {
        TournamentMap tournamentMap = new TournamentMap();
        tournamentMap.setTitle("Venue map");
        tournamentMap.setDescription("Rooms and entrances");
        tournamentMap.setTournament(tournament);
        Url imageUrl = new Url();
        imageUrl.setUrl("/uploads/images/tournament-maps/" + filename);
        tournamentMap.setImageUrl(imageUrl);
        return tournamentMap;
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Tournament map PostgreSQL migration");
        tournament.setDescription("Migration fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }
}
