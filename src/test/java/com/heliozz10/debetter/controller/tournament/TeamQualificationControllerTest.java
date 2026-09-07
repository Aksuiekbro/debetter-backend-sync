package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Do not make this test transactional: an outer test transaction would mask the endpoint's missing-transaction regression.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team_qualification_test;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/team-qualification"
})
@AutoConfigureMockMvc
@Import(TeamQualificationControllerTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TeamQualificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedRegenerationPreservesOriginalMatchesAndPublicationAfterDisqualification(boolean excludeEveryTeam) throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        var organizer = organizerWithRole(tournament, TournamentRole.EDIT);
        List<Team> teams = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            teams.add(teamRepository.saveAndFlush(team(tournament, excludeEveryTeam || index == 3)));
        }
        RoundGroup group = roundGroupRepository.saveAndFlush(new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.APF));
        Round round = new Round(group, "Round with disqualified entrants", 1);
        round.setTeams(teams);
        round.setMatchesArePublic(true);
        round.setMatches(new ArrayList<>());
        Match original = new Match();
        original.setRound(round);
        original.setTeam1(teams.get(0));
        original.setTeam2(teams.get(1));
        original.setCompleted(false);
        original.setIsBye(false);
        round.getMatches().add(original);
        round = roundRepository.saveAndFlush(round);

        mockMvc.perform(patch("/api/tournaments/{tournamentId}/round-groups/{groupId}/rounds/{roundId}/matches/randomize",
                        tournament.getId(), group.getId(), round.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(excludeEveryTeam ? "No eligible entrants" : "complete matches")));

        assertTrue(matchRepository.existsById(original.getId()));
        assertEquals(1, matchRepository.findByRoundId(round.getId()).size());
        assertTrue(roundRepository.findById(round.getId()).orElseThrow().getMatchesArePublic());
    }

    @Test
    void organizerWithEditPermissionCanDisqualifyTeam() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        Team team = teamRepository.saveAndFlush(team(tournament, false));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(tournament, TournamentRole.EDIT);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/disqualify", tournament.getId(), team.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isOk());

        assertTrue(teamRepository.findById(team.getId()).orElseThrow().getDisqualified());
        assertFalse(teamRepository.findByTournamentAndDisqualifiedFalse(tournament).stream()
                .anyMatch(eligibleTeam -> eligibleTeam.getId().equals(team.getId())));
    }

    @Test
    void organizerWithFullPermissionCanRequalifyTeam() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        Team team = teamRepository.saveAndFlush(team(tournament, true));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(tournament, TournamentRole.FULL);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/requalify", tournament.getId(), team.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isOk());

        assertFalse(teamRepository.findById(team.getId()).orElseThrow().getDisqualified());
        assertTrue(teamRepository.findByTournamentAndDisqualifiedFalse(tournament).stream()
                .anyMatch(eligibleTeam -> eligibleTeam.getId().equals(team.getId())));
    }

    @Test
    void organizerWithoutEditPermissionCannotDisqualifyTeam() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        Team team = teamRepository.saveAndFlush(team(tournament, false));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(tournament, TournamentRole.VIEW);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/disqualify", tournament.getId(), team.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isForbidden());

        assertFalse(teamRepository.findById(team.getId()).orElseThrow().getDisqualified());
    }

    @Test
    void organizerWithoutEditPermissionCannotRequalifyTeam() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        Team team = teamRepository.saveAndFlush(team(tournament, true));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(tournament, TournamentRole.VIEW);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/requalify", tournament.getId(), team.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isForbidden());

        assertTrue(teamRepository.findById(team.getId()).orElseThrow().getDisqualified());
    }

    @Test
    void disqualifyRejectsTeamFromAnotherTournament() throws Exception {
        Tournament authorizedTournament = tournamentRepository.saveAndFlush(tournament());
        Tournament otherTournament = tournamentRepository.saveAndFlush(tournament());
        Team otherTeam = teamRepository.saveAndFlush(team(otherTournament, false));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(authorizedTournament, TournamentRole.EDIT);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/disqualify",
                        authorizedTournament.getId(), otherTeam.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found"));

        assertFalse(teamRepository.findById(otherTeam.getId()).orElseThrow().getDisqualified());
    }

    @Test
    void requalifyRejectsTeamFromAnotherTournament() throws Exception {
        Tournament authorizedTournament = tournamentRepository.saveAndFlush(tournament());
        Tournament otherTournament = tournamentRepository.saveAndFlush(tournament());
        Team otherTeam = teamRepository.saveAndFlush(team(otherTournament, true));
        UsernamePasswordAuthenticationToken organizer = organizerWithRole(authorizedTournament, TournamentRole.EDIT);

        mockMvc.perform(patch("/api/tournaments/{id}/teams/{teamId}/requalify",
                        authorizedTournament.getId(), otherTeam.getId())
                        .servletPath("/api")
                        .with(authentication(organizer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found"));

        assertTrue(teamRepository.findById(otherTeam.getId()).orElseThrow().getDisqualified());
    }

    private UsernamePasswordAuthenticationToken organizerWithRole(
            Tournament tournament,
            TournamentRole tournamentRole
    ) {
        String username = "team-qualification-organizer-" + UUID.randomUUID();
        User organizer = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "Organizer",
                Role.ORGANIZER
        ));

        UserTournamentRole role = new UserTournamentRole();
        role.setId(new UserTournamentKey(organizer.getId(), tournament.getId()));
        role.setUser(organizer);
        role.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        role.setRole(tournamentRole);
        userTournamentRoleRepository.saveAndFlush(role);

        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Team qualification regression");
        tournament.setDescription("Regression fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(8);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    private static Team team(Tournament tournament, boolean disqualified) {
        Team team = new Team();
        team.setName("Qualification state team");
        team.setTournament(tournament);
        team.setActive(true);
        team.setCheckedIn(true);
        team.setDisqualified(disqualified);
        return team;
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
