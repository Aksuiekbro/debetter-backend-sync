package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
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
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoundMatchResultsVisibilityReadTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void roundDetailUsesTheSamePublishedAndExactResultVisibilityAsMatches() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(fixture.endpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].completed").value(true))
                .andExpect(jsonPath("$.matches[0].team1Score").value(nullValue()))
                .andExpect(jsonPath("$.matches[0].team2Score").value(nullValue()))
                .andExpect(jsonPath("$.matches[0].team1Won").value(true))
                .andExpect(jsonPath("$.matches[0].team2Won").value(false));

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(grantFullAccess(fixture.tournament()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].team1Score").value(76))
                .andExpect(jsonPath("$.matches[0].team2Score").value(70))
                .andExpect(jsonPath("$.matches[0].team1Won").value(true))
                .andExpect(jsonPath("$.matches[0].team2Won").value(false));

        entityManager.createQuery("update Tournament t set t.disabled = true where t.id = :id")
                .setParameter("id", fixture.tournament().getId())
                .executeUpdate();
        entityManager.clear();

        mockMvc.perform(get(fixture.endpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].completed").value(true))
                .andExpect(jsonPath("$.matches[0].team1Score").value(nullValue()))
                .andExpect(jsonPath("$.matches[0].team2Score").value(nullValue()))
                .andExpect(jsonPath("$.matches[0].team1Won").value(nullValue()))
                .andExpect(jsonPath("$.matches[0].team2Won").value(nullValue()));
    }

    private Fixture fixture() {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        RoundGroup roundGroup = roundGroupRepository.saveAndFlush(
                new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.APF)
        );
        Round round = new Round(roundGroup, "Round 1", 1);
        round.setMatchesArePublic(true);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        Team team1 = teamRepository.saveAndFlush(team(tournament, "Affirmative"));
        Team team2 = teamRepository.saveAndFlush(team(tournament, "Negative"));
        Match match = new Match();
        match.setRound(round);
        match.setTeam1(team1);
        match.setTeam2(team2);
        match.setTeam1Score(76);
        match.setTeam2Score(70);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        match.setCompleted(true);
        match.setIsBye(false);
        matchRepository.saveAndFlush(match);
        entityManager.clear();

        return new Fixture(
                "/api/tournaments/" + tournament.getId()
                        + "/round-groups/" + roundGroup.getId()
                        + "/rounds/" + round.getId(),
                tournament
        );
    }

    private UsernamePasswordAuthenticationToken grantFullAccess(Tournament tournament) {
        String username = "round-results-organizer-" + UUID.randomUUID();
        User organizer = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Round",
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

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Round result visibility");
        tournament.setDescription("Round result visibility fixture");
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

    private static Team team(Tournament tournament, String name) {
        Team team = new Team();
        team.setName(name);
        team.setTournament(tournament);
        team.setActive(true);
        team.setCheckedIn(true);
        team.setDisqualified(false);
        return team;
    }

    private record Fixture(String endpoint, Tournament tournament) {
    }
}
