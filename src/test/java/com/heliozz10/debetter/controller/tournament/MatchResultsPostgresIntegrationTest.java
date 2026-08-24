package com.heliozz10.debetter.controller.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.ParticipantScoreDto;
import com.heliozz10.debetter.dto.tournament.match.in.TeamResultDto;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchParticipantScoreRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the complete result-submission path against the PostgreSQL function that production uses.
 *
 * <p>The deployed database predates Liquibase and is Hibernate-managed, so this test deliberately mirrors
 * that upgrade path: Hibernate creates the baseline tables, then the complete Liquibase master is applied
 * before any fixture is inserted. This catches both an invalid changelog and repository/JDBC invocation
 * regressions such as using {@code executeUpdate()} for the result-returning function SELECT.</p>
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/results-postgresql"
})
@AutoConfigureMockMvc
@Import(MatchResultsPostgresIntegrationTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchResultsPostgresIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.3-alpine3.20");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        // Spring resolves dynamic datasource properties while it is still building the
        // application context. Start explicitly here so getJdbcUrl never races the
        // JUnit Testcontainers extension on Spring Boot 4.
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantScoreRepository matchParticipantScoreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @BeforeAll
    void applyFullLiquibaseMasterToHibernateBaseline() throws LiquibaseException {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yml");
        liquibase.setAnalyticsEnabled(false);
        liquibase.setShouldRun(true);
        liquibase.afterPropertiesSet();

        String functionDefinition = jdbcTemplate.queryForObject(
                "SELECT pg_get_functiondef('update_match_scores_bulk(jsonb)'::regprocedure)",
                String.class
        );
        assertTrue(functionDefinition != null && functionDefinition.contains("rg.type = 'PRELIMINARY'"));
    }

    @Test
    void authorizedApfSubmissionPersistsScoresWinnersParticipantsAndStandingsAfterRefresh() throws Exception {
        TeamRoundFixture fixture = teamRoundFixture(DebateFormat.APF, 1);
        TeamMatchFixture match = fixture.matches().getFirst();
        MatchResultDto result = teamResult(match, List.of(
                List.of(70, 71),
                List.of(72, 73)
        ), List.of(true, false));

        submit(fixture.endpoint(), fixture.organizer(), List.of(result))
                .andExpect(status().isOk());

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(fixture.organizer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].team1Score").value(141))
                .andExpect(jsonPath("$.content[0].team2Score").value(145))
                .andExpect(jsonPath("$.content[0].team1Won").value(true))
                .andExpect(jsonPath("$.content[0].team2Won").value(false))
                .andExpect(jsonPath("$.content[0].team1ParticipantScores.length()").value(2))
                .andExpect(jsonPath("$.content[0].team2ParticipantScores.length()").value(2));

        assertEquals(141, reloadedTeam(match, 0).getPreliminaryScore());
        assertEquals(145, reloadedTeam(match, 1).getPreliminaryScore());
        assertParticipantScores(match, List.of(70, 71, 72, 73));
        assertEquals(4, matchParticipantScoreRepository.countByMatchId(match.matchId()));
    }

    @Test
    void authorizedBpfSubmissionPersistsFourTeamBallotAfterRefresh() throws Exception {
        TeamRoundFixture fixture = teamRoundFixture(DebateFormat.BPF, 1);
        TeamMatchFixture match = fixture.matches().getFirst();
        MatchResultDto result = teamResult(match, List.of(
                List.of(70, 71),
                List.of(72, 73),
                List.of(74, 75),
                List.of(76, 77)
        ), List.of(true, true, false, false));

        submit(fixture.endpoint(), fixture.organizer(), List.of(result))
                .andExpect(status().isOk());

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(fixture.organizer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].team1Score").value(141))
                .andExpect(jsonPath("$.content[0].team2Score").value(145))
                .andExpect(jsonPath("$.content[0].team3Score").value(149))
                .andExpect(jsonPath("$.content[0].team4Score").value(153))
                .andExpect(jsonPath("$.content[0].team1Won").value(true))
                .andExpect(jsonPath("$.content[0].team2Won").value(true))
                .andExpect(jsonPath("$.content[0].team3Won").value(false))
                .andExpect(jsonPath("$.content[0].team4Won").value(false))
                .andExpect(jsonPath("$.content[0].team4ParticipantScores.length()").value(2));

        assertEquals(141, reloadedTeam(match, 0).getPreliminaryScore());
        assertEquals(145, reloadedTeam(match, 1).getPreliminaryScore());
        assertEquals(149, reloadedTeam(match, 2).getPreliminaryScore());
        assertEquals(153, reloadedTeam(match, 3).getPreliminaryScore());
        assertParticipantScores(match, List.of(70, 71, 72, 73, 74, 75, 76, 77));
        assertEquals(8, matchParticipantScoreRepository.countByMatchId(match.matchId()));
    }

    @Test
    void authorizedPreliminaryLdSubmissionPersistsMatchAndSpeakerScoresAfterRefresh() throws Exception {
        LdRoundFixture fixture = ldRoundFixture();
        MatchResultDto result = new MatchResultDto(
                fixture.matchId(),
                null,
                List.of(
                        new ParticipantScoreDto(fixture.debater1().getId(), 80),
                        new ParticipantScoreDto(fixture.debater2().getId(), 75)
                )
        );

        submit(fixture.endpoint(), fixture.organizer(), List.of(result))
                .andExpect(status().isOk());

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(fixture.organizer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].debater1Score").value(80))
                .andExpect(jsonPath("$.content[0].debater2Score").value(75))
                .andExpect(jsonPath("$.content[0].debater1.speakerScore").value(80))
                .andExpect(jsonPath("$.content[0].debater2.speakerScore").value(75));

        assertEquals(80, reloadedParticipant(fixture.debater1()).getSpeakerScore());
        assertEquals(75, reloadedParticipant(fixture.debater2()).getSpeakerScore());
    }

    @Test
    void invalidIncompleteBallotReturnsDescriptiveBadRequestWithoutWriting() throws Exception {
        TeamRoundFixture fixture = teamRoundFixture(DebateFormat.APF, 1);
        TeamMatchFixture match = fixture.matches().getFirst();
        MatchResultDto incomplete = new MatchResultDto(
                match.matchId(),
                List.of(new TeamResultDto(
                        match.teams().getFirst().getId(),
                        true,
                        participantScores(match.participantsByTeam().getFirst(), List.of(70, 71))
                )),
                null
        );

        submit(fixture.endpoint(), fixture.organizer(), List.of(incomplete))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A result is required for every team in the match."));

        Match reloaded = matchRepository.findById(match.matchId()).orElseThrow();
        assertFalse(reloaded.getCompleted());
        assertNull(reloaded.getTeam1Score());
        assertNull(reloaded.getTeam1Won());
        assertEquals(0, matchParticipantScoreRepository.countByMatchId(match.matchId()));
    }

    @Test
    void multiMatchSubmissionRollsBackEveryWriteWhenLateParticipantInsertFails() throws Exception {
        TeamRoundFixture fixture = teamRoundFixture(DebateFormat.APF, 2);
        TeamMatchFixture first = fixture.matches().get(0);
        TeamMatchFixture second = fixture.matches().get(1);
        jdbcTemplate.execute("""
                ALTER TABLE match_participant_score
                ADD CONSTRAINT issue6_reject_sentinel_score CHECK (score <> 999)
                """);

        MatchResultDto validFirst = teamResult(first, List.of(
                List.of(70, 71),
                List.of(72, 73)
        ), List.of(true, false));
        MatchResultDto lateFailure = teamResult(second, List.of(
                List.of(999, 1),
                List.of(60, 61)
        ), List.of(true, false));

        submit(fixture.endpoint(), fixture.organizer(), List.of(validFirst, lateFailure))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This change conflicts with existing data. Please refresh the page and try again."
                ));

        for (TeamMatchFixture submittedMatch : fixture.matches()) {
            Match reloaded = matchRepository.findById(submittedMatch.matchId()).orElseThrow();
            assertFalse(reloaded.getCompleted());
            assertNull(reloaded.getTeam1Score());
            assertNull(reloaded.getTeam2Score());
            assertNull(reloaded.getTeam1Won());
            assertNull(reloaded.getTeam2Won());
            assertEquals(0, matchParticipantScoreRepository.countByMatchId(submittedMatch.matchId()));

            submittedMatch.teams().forEach(team ->
                    assertEquals(0, teamRepository.findById(team.getId()).orElseThrow().getPreliminaryScore())
            );
            submittedMatch.participantsByTeam().stream()
                    .flatMap(List::stream)
                    .forEach(participant ->
                            assertEquals(0, reloadedParticipant(participant).getSpeakerScore())
                    );
        }
    }

    private org.springframework.test.web.servlet.ResultActions submit(
            String endpoint,
            UsernamePasswordAuthenticationToken organizer,
            List<MatchResultDto> results
    ) throws Exception {
        return mockMvc.perform(patch(endpoint + "/results")
                .servletPath("/api")
                .with(authentication(organizer))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(results)));
    }

    private TeamRoundFixture teamRoundFixture(DebateFormat format, int matchCount) {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament(format));
        RoundGroup roundGroup = roundGroupRepository.saveAndFlush(
                new RoundGroup(tournament, RoundGroupType.PRELIMINARY, format)
        );
        roundGroup.setCurrentRoundNumber(1);

        Round round = new Round(roundGroup, format + " preliminary", 1);
        round.setMatchesArePublic(true);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        int teamCount = format == DebateFormat.BPF ? 4 : 2;
        List<TeamMatchFixture> matches = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < matchCount; matchIndex++) {
            List<Team> teams = new ArrayList<>();
            List<List<TournamentParticipant>> participantsByTeam = new ArrayList<>();
            for (int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
                Team team = teamRepository.saveAndFlush(team(
                        tournament,
                        format + " match " + matchIndex + " team " + teamIndex
                ));
                List<TournamentParticipant> participants = List.of(
                        tournamentParticipantRepository.saveAndFlush(participant(team)),
                        tournamentParticipantRepository.saveAndFlush(participant(team))
                );
                teams.add(team);
                participantsByTeam.add(participants);
            }

            Match match = new Match();
            match.setRound(round);
            match.setTeam1(teams.get(0));
            match.setTeam2(teams.get(1));
            if (format == DebateFormat.BPF) {
                match.setTeam3(teams.get(2));
                match.setTeam4(teams.get(3));
            }
            match.setCompleted(false);
            match.setIsBye(false);
            match = matchRepository.saveAndFlush(match);
            matches.add(new TeamMatchFixture(match.getId(), teams, participantsByTeam));
        }

        return new TeamRoundFixture(
                endpoint(tournament, roundGroup, round),
                organizerWithFullAccess(tournament),
                matches
        );
    }

    private LdRoundFixture ldRoundFixture() {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament(DebateFormat.LD));
        RoundGroup roundGroup = roundGroupRepository.saveAndFlush(
                new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.LD)
        );
        roundGroup.setCurrentRoundNumber(1);

        Round round = new Round(roundGroup, "LD preliminary", 1);
        round.setMatchesArePublic(true);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        TournamentParticipant debater1 = tournamentParticipantRepository.saveAndFlush(participant(null));
        TournamentParticipant debater2 = tournamentParticipantRepository.saveAndFlush(participant(null));
        Match match = new Match();
        match.setRound(round);
        match.setDebater1(debater1);
        match.setDebater2(debater2);
        match.setCompleted(false);
        match.setIsBye(false);
        match = matchRepository.saveAndFlush(match);

        return new LdRoundFixture(
                endpoint(tournament, roundGroup, round),
                organizerWithFullAccess(tournament),
                match.getId(),
                debater1,
                debater2
        );
    }

    private MatchResultDto teamResult(
            TeamMatchFixture match,
            List<List<Integer>> scoresByTeam,
            List<Boolean> winners
    ) {
        List<TeamResultDto> teamResults = new ArrayList<>();
        for (int teamIndex = 0; teamIndex < match.teams().size(); teamIndex++) {
            teamResults.add(new TeamResultDto(
                    match.teams().get(teamIndex).getId(),
                    winners.get(teamIndex),
                    participantScores(match.participantsByTeam().get(teamIndex), scoresByTeam.get(teamIndex))
            ));
        }
        return new MatchResultDto(match.matchId(), teamResults, null);
    }

    private List<ParticipantScoreDto> participantScores(
            List<TournamentParticipant> participants,
            List<Integer> scores
    ) {
        List<ParticipantScoreDto> result = new ArrayList<>();
        for (int index = 0; index < participants.size(); index++) {
            result.add(new ParticipantScoreDto(participants.get(index).getId(), scores.get(index)));
        }
        return result;
    }

    private Team reloadedTeam(TeamMatchFixture match, int index) {
        return teamRepository.findById(match.teams().get(index).getId()).orElseThrow();
    }

    private TournamentParticipant reloadedParticipant(TournamentParticipant participant) {
        return tournamentParticipantRepository.findById(participant.getId()).orElseThrow();
    }

    private void assertParticipantScores(TeamMatchFixture match, List<Integer> expectedScores) {
        List<Integer> actualScores = match.participantsByTeam().stream()
                .flatMap(List::stream)
                .map(this::reloadedParticipant)
                .map(TournamentParticipant::getSpeakerScore)
                .toList();
        assertEquals(expectedScores, actualScores);
    }

    private UsernamePasswordAuthenticationToken organizerWithFullAccess(Tournament tournament) {
        String suffix = UUID.randomUUID().toString();
        User organizer = userRepository.saveAndFlush(new User(
                "results-organizer-" + suffix,
                UUID.randomUUID().toString(),
                "results-organizer-" + suffix + "@example.invalid",
                "Results",
                "Organizer",
                Role.ORGANIZER
        ));
        UserTournamentRole role = new UserTournamentRole();
        role.setId(new UserTournamentKey(organizer.getId(), tournament.getId()));
        role.setUser(organizer);
        role.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        role.setRole(TournamentRole.FULL);
        userTournamentRoleRepository.saveAndFlush(role);
        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }

    private static String endpoint(Tournament tournament, RoundGroup roundGroup, Round round) {
        return "/api/tournaments/" + tournament.getId()
                + "/round-groups/" + roundGroup.getId()
                + "/rounds/" + round.getId()
                + "/matches";
    }

    private static Tournament tournament(DebateFormat format) {
        Tournament tournament = new Tournament();
        tournament.setName("PostgreSQL results regression " + UUID.randomUUID());
        tournament.setDescription("Issue #6 integration fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(format);
        tournament.setTeamEliminationFormat(format == DebateFormat.LD ? DebateFormat.APF : format);
        tournament.setStarted(true);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    private static Team team(Tournament tournament, String name) {
        Team team = new Team();
        team.setName(name);
        team.setTournament(tournament);
        team.setPreliminaryScore(0);
        team.setActive(true);
        team.setCheckedIn(true);
        team.setDisqualified(false);
        return team;
    }

    private static TournamentParticipant participant(Team team) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setTeam(team);
        participant.setSpeakerScore(0);
        return participant;
    }

    private record TeamRoundFixture(
            String endpoint,
            UsernamePasswordAuthenticationToken organizer,
            List<TeamMatchFixture> matches
    ) {
    }

    private record TeamMatchFixture(
            Long matchId,
            List<Team> teams,
            List<List<TournamentParticipant>> participantsByTeam
    ) {
    }

    private record LdRoundFixture(
            String endpoint,
            UsernamePasswordAuthenticationToken organizer,
            Long matchId,
            TournamentParticipant debater1,
            TournamentParticipant debater2
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("userTournamentPermissions");
        }
    }
}
