package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:my_tournaments_test;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/my-tournaments"
})
@AutoConfigureMockMvc
@Transactional
class MyTournamentControllerTest {
    private static final String ENDPOINT = "/api/tournaments/mine";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Test
    void returnsOnlyPrincipalMembershipsWithRoleAwareHiddenVisibility() throws Exception {
        User principal = saveUser("member");
        User anotherUser = saveUser("other-member");

        addMembership(principal, saveTournament("Full visible", false, 1), TournamentRole.FULL);
        addMembership(principal, saveTournament("Edit visible", false, 2), TournamentRole.EDIT);
        addMembership(principal, saveTournament("View visible", false, 3), TournamentRole.VIEW);
        addMembership(principal, saveTournament("Full hidden", true, 4), TournamentRole.FULL);
        addMembership(principal, saveTournament("Edit hidden", true, 5), TournamentRole.EDIT);
        addMembership(principal, saveTournament("View hidden", true, 6), TournamentRole.VIEW);
        addMembership(anotherUser, saveTournament("Unrelated", false, 7), TournamentRole.FULL);

        // Pending invitations have no UserTournamentRole until acceptance.
        saveTournament("Pending invitation", false, 8);

        mockMvc.perform(get(ENDPOINT)
                        .servletPath("/api")
                        .param("size", "20")
                        .param("sort", "name,asc")
                        .with(authentication(authenticationFor(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[*].name").value(contains(
                        "Edit hidden",
                        "Edit visible",
                        "Full hidden",
                        "Full visible",
                        "View visible"
                )))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("View hidden"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Unrelated"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Pending invitation"))));
    }

    @Test
    void newlyAcceptedRoleAppearsOnTheNextRead() throws Exception {
        User principal = saveUser("new-member");
        Tournament tournament = saveTournament("Accepted after refresh", false, 1);

        mockMvc.perform(get(ENDPOINT)
                        .servletPath("/api")
                        .with(authentication(authenticationFor(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        addMembership(principal, tournament, TournamentRole.VIEW);

        mockMvc.perform(get(ENDPOINT)
                        .servletPath("/api")
                        .with(authentication(authenticationFor(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Accepted after refresh"));
    }

    @Test
    void appliesExistingFiltersPaginationAndSortingBeforeBuildingTheResponse() throws Exception {
        User principal = saveUser("filtered-member");
        addMembership(principal, saveTournament("Earlier qualifying", false, 5), TournamentRole.VIEW);
        addMembership(principal, saveTournament("Later qualifying", false, 10), TournamentRole.EDIT);
        addMembership(principal, saveTournament("Too early", false, 1), TournamentRole.FULL);

        String startDateFrom = LocalDateTime.now().plusDays(4).withNano(0).toString();

        mockMvc.perform(get(ENDPOINT)
                        .servletPath("/api")
                        .param("startDateFrom", startDateFrom)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "startDate,asc")
                        .with(authentication(authenticationFor(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Earlier qualifying"));
    }

    @Test
    void ignoresArbitraryUserIdAndAlwaysUsesTheAuthenticatedPrincipal() throws Exception {
        User principal = saveUser("principal-member");
        User anotherUser = saveUser("requested-member");
        addMembership(principal, saveTournament("Principal tournament", false, 1), TournamentRole.VIEW);
        addMembership(anotherUser, saveTournament("Other tournament", false, 2), TournamentRole.FULL);

        mockMvc.perform(get(ENDPOINT)
                        .servletPath("/api")
                        .param("userId", anotherUser.getId().toString())
                        .with(authentication(authenticationFor(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Principal tournament"));
    }

    @Test
    void deniesAnonymousRequestsEvenThoughPublicTournamentGetsArePermitted() throws Exception {
        mockMvc.perform(get(ENDPOINT).servletPath("/api"))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String prefix) {
        String unique = prefix + "-" + UUID.randomUUID();
        return userRepository.saveAndFlush(new User(
                unique,
                UUID.randomUUID().toString(),
                unique + "@example.invalid",
                "Test",
                "Member",
                Role.PARTICIPANT
        ));
    }

    private Tournament saveTournament(String name, boolean hidden, int startDayOffset) {
        LocalDateTime startDate = LocalDateTime.now().plusDays(startDayOffset).withNano(0);
        Tournament tournament = new Tournament();
        tournament.setName(name);
        tournament.setDescription("Membership query fixture");
        tournament.setStartDate(startDate);
        tournament.setEndDate(startDate.plusDays(1));
        tournament.setRegistrationDeadline(startDate.minusDays(1));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(hidden);
        return tournamentRepository.saveAndFlush(tournament);
    }

    private void addMembership(User user, Tournament tournament, TournamentRole tournamentRole) {
        UserTournamentRole membership = new UserTournamentRole();
        membership.setId(new UserTournamentKey(user.getId(), tournament.getId()));
        membership.setUser(userRepository.getReferenceById(user.getId()));
        membership.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        membership.setRole(tournamentRole);
        userTournamentRoleRepository.saveAndFlush(membership);
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }
}
