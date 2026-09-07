package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JudgeContactPrivacyControllerTest.CacheTestConfiguration.class)
@Transactional
class JudgeContactPrivacyControllerTest {
    private static final String EMAIL = "judge@example.invalid";
    private static final String PHONE_NUMBER = "+77010000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private JudgeRepository judgeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Test
    void guestParticipantAndUnrelatedOrganizerNeverReceiveContactDetails() throws Exception {
        Fixture fixture = fixture();
        UsernamePasswordAuthenticationToken participant = authenticationFor(
                fixture.tournament(),
                Role.PARTICIPANT,
                TournamentRole.VIEW
        );
        UsernamePasswordAuthenticationToken unrelatedOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                null
        );

        mockMvc.perform(get(fixture.judgesEndpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Aigerim Judge"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].phoneNumber").doesNotExist());

        mockMvc.perform(get(fixture.judgeEndpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].phoneNumber").doesNotExist());

        mockMvc.perform(get(fixture.judgeEndpoint())
                        .servletPath("/api")
                        .with(authentication(unrelatedOrganizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());
    }

    @Test
    void editAndFullOrganizersReceiveExactContactDetails() throws Exception {
        Fixture fixture = fixture();
        UsernamePasswordAuthenticationToken editOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                TournamentRole.EDIT
        );
        UsernamePasswordAuthenticationToken fullOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                TournamentRole.FULL
        );

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(editOrganizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(PHONE_NUMBER));

        mockMvc.perform(get(fixture.judgeEndpoint())
                        .servletPath("/api")
                        .with(authentication(fullOrganizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER));
    }

    @Test
    void privateFiltersAndSortsAreForbiddenForEveryUnauthorizedCaller() throws Exception {
        Fixture fixture = fixture();
        UsernamePasswordAuthenticationToken participant = authenticationFor(
                fixture.tournament(),
                Role.PARTICIPANT,
                TournamentRole.VIEW
        );
        UsernamePasswordAuthenticationToken unrelatedOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                null
        );

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .param("searchEmail", "judge"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .param("sort", "email,asc"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(participant))
                        .param("phoneNumber", PHONE_NUMBER))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(participant))
                        .param("sort", "phoneNumber,desc"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(unrelatedOrganizer))
                        .param("searchEmail", "judge"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(unrelatedOrganizer))
                        .param("sort", "fullName,asc", "phoneNumber,asc"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "fullName", "checkedIn"})
    void publicSortWhitelistRemainsAvailable(String property) throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .param("sort", property + ",asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void editAndFullOrganizersCanUsePrivateFiltersAndSorts() throws Exception {
        Fixture fixture = fixture();
        judgeRepository.saveAndFlush(judge(
                fixture.tournament(),
                "Zhan Judge",
                "zhan@example.invalid",
                "+77020000000"
        ));
        UsernamePasswordAuthenticationToken editOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                TournamentRole.EDIT
        );
        UsernamePasswordAuthenticationToken fullOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                TournamentRole.FULL
        );

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(editOrganizer))
                        .param("searchEmail", "judge"))
                .andExpect(status().isOk());

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(editOrganizer))
                        .param("sort", "email,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL));

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(fullOrganizer))
                        .param("phoneNumber", PHONE_NUMBER)
                        .param("sort", "phoneNumber,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(PHONE_NUMBER));
    }

    @Test
    void unsupportedSortPropertiesReturnBadRequestForPublicAndOrganizerCalls() throws Exception {
        Fixture fixture = fixture();
        UsernamePasswordAuthenticationToken editOrganizer = authenticationFor(
                fixture.tournament(),
                Role.ORGANIZER,
                TournamentRole.EDIT
        );

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .param("sort", "timesJudged,asc"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(fixture.judgesEndpoint())
                        .servletPath("/api")
                        .with(authentication(editOrganizer))
                        .param("sort", "tournament.id,asc"))
                .andExpect(status().isBadRequest());
    }

    private Fixture fixture() {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        Judge judge = judgeRepository.saveAndFlush(judge(
                tournament,
                "Aigerim Judge",
                EMAIL,
                PHONE_NUMBER
        ));
        return new Fixture(tournament, judge);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(
            Tournament tournament,
            Role globalRole,
            TournamentRole tournamentRole
    ) {
        String username = "judge-privacy-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "User",
                globalRole
        ));

        if (tournamentRole != null) {
            UserTournamentRole role = new UserTournamentRole();
            role.setId(new UserTournamentKey(user.getId(), tournament.getId()));
            role.setUser(user);
            role.setTournament(tournament);
            role.setRole(tournamentRole);
            userTournamentRoleRepository.saveAndFlush(role);
        }

        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Judge Privacy Cup");
        tournament.setDescription("Judge contact privacy regression fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(16);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    private static Judge judge(
            Tournament tournament,
            String fullName,
            String email,
            String phoneNumber
    ) {
        Judge judge = new Judge();
        judge.setTournament(tournament);
        judge.setFullName(fullName);
        judge.setEmail(email);
        judge.setPhoneNumber(phoneNumber);
        judge.setCheckedIn(false);
        judge.setTimesJudged(0);
        return judge;
    }

    private record Fixture(Tournament tournament, Judge judge) {
        private String judgesEndpoint() {
            return "/api/tournaments/" + tournament.getId() + "/judges";
        }

        private String judgeEndpoint() {
            return judgesEndpoint() + "/" + judge.getId();
        }
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
