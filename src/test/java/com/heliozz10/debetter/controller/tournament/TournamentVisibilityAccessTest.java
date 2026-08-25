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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TournamentVisibilityAccessTest {
    private static final LocalDateTime FILTER_START = LocalDateTime.of(2099, 5, 1, 0, 0);
    private static final LocalDateTime FILTER_END = LocalDateTime.of(2099, 5, 31, 23, 59);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void publicListOmitsHiddenRowsBeforePaginationAndKeepsLegacyNullRowsVisible() throws Exception {
        tournamentRepository.saveAndFlush(tournament("A Hidden Cup", true));
        tournamentRepository.saveAndFlush(tournament("B Visible Cup", false));
        tournamentRepository.saveAndFlush(tournament("C Legacy Cup", null));
        entityManager.clear();

        mockMvc.perform(discoveryRequest(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("B Visible Cup"));

        mockMvc.perform(discoveryRequest(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("C Legacy Cup"));
    }

    @ParameterizedTest(name = "hidden route {0} rejects guests and VIEW members")
    @MethodSource("hiddenTournamentReadPaths")
    void hiddenTournamentAndNestedReadsRejectGuestAndView(String pathTemplate) throws Exception {
        Tournament hidden = tournamentRepository.saveAndFlush(tournament("Hidden Route Cup", true));
        UsernamePasswordAuthenticationToken viewer = grant(hidden, TournamentRole.VIEW, Role.PARTICIPANT);
        String path = pathTemplate.formatted(hidden.getId());

        mockMvc.perform(get(path).servletPath("/api"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Hidden Route Cup"))));

        mockMvc.perform(get(path)
                        .servletPath("/api")
                        .with(authentication(viewer)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Hidden Route Cup"))));
    }

    @ParameterizedTest
    @EnumSource(value = TournamentRole.class, names = {"EDIT", "FULL"})
    void editAndFullMembersCanReadHiddenTournament(TournamentRole role) throws Exception {
        Tournament hidden = tournamentRepository.saveAndFlush(tournament("Organizer Hidden Cup", true));

        mockMvc.perform(get("/api/tournaments/{id}", hidden.getId())
                        .servletPath("/api")
                        .with(authentication(grant(hidden, role, Role.ORGANIZER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Organizer Hidden Cup"));
    }

    @Test
    void unrelatedOrganizerCannotReadHiddenTournament() throws Exception {
        Tournament hidden = tournamentRepository.saveAndFlush(tournament("Private Organizer Cup", true));
        User unrelatedOrganizer = userRepository.saveAndFlush(user(Role.ORGANIZER));

        mockMvc.perform(get("/api/tournaments/{id}", hidden.getId())
                        .servletPath("/api")
                        .with(authentication(tokenFor(unrelatedOrganizer))))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyFullMemberCanToggleVisibilityAndReenabledTournamentIsPublicAgain() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament("Toggle Cup", false));
        UsernamePasswordAuthenticationToken editor = grant(tournament, TournamentRole.EDIT, Role.ORGANIZER);
        UsernamePasswordAuthenticationToken owner = grant(tournament, TournamentRole.FULL, Role.ORGANIZER);

        mockMvc.perform(patch("/api/tournaments/{id}/disable", tournament.getId())
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tournaments/{id}/disable", tournament.getId())
                        .servletPath("/api")
                        .with(authentication(owner)))
                .andExpect(status().isOk());
        entityManager.clear();

        mockMvc.perform(get("/api/tournaments/{id}", tournament.getId()).servletPath("/api"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tournaments/{id}", tournament.getId())
                        .servletPath("/api")
                        .with(authentication(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tournaments/{id}/enable", tournament.getId())
                        .servletPath("/api")
                        .with(authentication(owner)))
                .andExpect(status().isOk());
        entityManager.clear();

        mockMvc.perform(get("/api/tournaments/{id}", tournament.getId()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabled").value(false));
    }

    @Test
    void missingTournamentStillUsesNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/tournaments/{id}", Long.MAX_VALUE).servletPath("/api"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder discoveryRequest(int page) {
        return get("/api/tournaments")
                .servletPath("/api")
                .queryParam("startDateFrom", FILTER_START.toString())
                .queryParam("startDateTo", FILTER_END.toString())
                .queryParam("page", Integer.toString(page))
                .queryParam("size", "1")
                .queryParam("sort", "name,asc");
    }

    private UsernamePasswordAuthenticationToken grant(
            Tournament tournament,
            TournamentRole tournamentRole,
            Role accountRole
    ) {
        User user = userRepository.saveAndFlush(user(accountRole));
        UserTournamentRole role = new UserTournamentRole();
        role.setId(new UserTournamentKey(user.getId(), tournament.getId()));
        role.setUser(user);
        role.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        role.setRole(tournamentRole);
        userTournamentRoleRepository.saveAndFlush(role);
        return tokenFor(user);
    }

    private static UsernamePasswordAuthenticationToken tokenFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static User user(Role role) {
        String username = "visibility-" + UUID.randomUUID();
        return new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Visibility",
                "Tester",
                role
        );
    }

    private static Tournament tournament(String name, Boolean disabled) {
        Tournament tournament = new Tournament();
        tournament.setName(name);
        tournament.setDescription("Visibility access fixture");
        tournament.setStartDate(FILTER_START.plusDays(7));
        tournament.setEndDate(FILTER_START.plusDays(8));
        tournament.setRegistrationDeadline(FILTER_START.plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(disabled);
        return tournament;
    }

    private static Stream<String> hiddenTournamentReadPaths() {
        return Stream.of(
                "/api/tournaments/%d",
                "/api/tournaments/%d/main-organizer",
                "/api/tournaments/%d/organizers",
                "/api/tournaments/%d/participants",
                "/api/tournaments/%d/participants/999999",
                "/api/tournaments/%d/teams",
                "/api/tournaments/%d/teams/999999",
                "/api/tournaments/%d/announcements",
                "/api/tournaments/%d/announcements/999999",
                "/api/tournaments/%d/announcements/999999/comments",
                "/api/tournaments/%d/schedules",
                "/api/tournaments/%d/schedules/999999",
                "/api/tournaments/%d/judges",
                "/api/tournaments/%d/judges/999999",
                "/api/tournaments/%d/feedbacks",
                "/api/tournaments/%d/feedbacks/999999",
                "/api/tournaments/%d/round-groups",
                "/api/tournaments/%d/round-groups/999999/rounds",
                "/api/tournaments/%d/round-groups/999999/rounds/999999",
                "/api/tournaments/%d/round-groups/999999/rounds/999999/matches"
        );
    }
}
