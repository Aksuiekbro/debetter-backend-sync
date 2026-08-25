package com.heliozz10.debetter.controller.util.request;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.repository.util.request.ParticipantInvitationRepository;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:participant_invitation_contract_test;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/participant-invitation-contract"
})
@AutoConfigureMockMvc
@Import(ParticipantInvitationControllerContractTest.CacheTestConfiguration.class)
@Transactional
class ParticipantInvitationControllerContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipantProfileRepository participantProfileRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Autowired
    private ParticipantInvitationRepository participantInvitationRepository;

    @Test
    void differentParticipantCannotAcceptOrRejectAnotherParticipantsInvitation() throws Exception {
        InvitationFixture fixture = invitationFixture();

        mockMvc.perform(post("/api/participant-invitations/{id}/accept", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.otherUser()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/participant-invitations/{id}/reject", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.otherUser()))))
                .andExpect(status().isNotFound());

        ParticipantInvitation pending = participantInvitationRepository.findById(fixture.invitation().getId()).orElseThrow();
        assertFalse(pending.getAccepted());
        assertFalse(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(
                fixture.tournament().getId(),
                fixture.inviteeProfile().getId()
        ));
    }

    @Test
    void receivedInvitationIncludesTournamentTeamAndServicePopulatedUsers() throws Exception {
        InvitationFixture fixture = invitationFixture();

        mockMvc.perform(get("/api/participant-invitations/received")
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.inviteeUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tournament.id").value(fixture.tournament().getId()))
                .andExpect(jsonPath("$.content[0].tournament.name").value(fixture.tournament().getName()))
                .andExpect(jsonPath("$.content[0].team.id").value(fixture.invitation().getTeam().getId()))
                .andExpect(jsonPath("$.content[0].team.name").value(fixture.invitation().getTeam().getName()))
                .andExpect(jsonPath("$.content[0].inviter.id").value(fixture.inviterUser().getId()))
                .andExpect(jsonPath("$.content[0].inviter.username").value(fixture.inviterUser().getUsername()))
                .andExpect(jsonPath("$.content[0].invitee.id").value(fixture.inviteeUser().getId()))
                .andExpect(jsonPath("$.content[0].invitee.username").value(fixture.inviteeUser().getUsername()));
    }

    @Test
    void acceptingInvitationAddsTeamMembershipAndViewRole() throws Exception {
        InvitationFixture fixture = invitationFixture();

        mockMvc.perform(post("/api/participant-invitations/{id}/accept", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.inviteeUser()))))
                .andExpect(status().isOk());

        assertTrue(participantInvitationRepository.findById(fixture.invitation().getId()).orElseThrow().getAccepted());
        assertTrue(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(
                fixture.tournament().getId(),
                fixture.inviteeProfile().getId()
        ));
        assertTrue(userTournamentRoleRepository.findRolesByUserIdAndTournamentId(
                fixture.inviteeUser().getId(),
                fixture.tournament().getId()
        ).contains(TournamentRole.VIEW));
    }

    @Test
    void acceptedInvitationCannotBeRejectedAndKeepsGrantedAccess() throws Exception {
        InvitationFixture fixture = invitationFixture();

        mockMvc.perform(post("/api/participant-invitations/{id}/accept", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.inviteeUser()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/participant-invitations/{id}/reject", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.inviteeUser()))))
                .andExpect(status().isBadRequest());

        assertTrue(participantInvitationRepository.findById(fixture.invitation().getId()).orElseThrow().getAccepted());
        assertTrue(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(
                fixture.tournament().getId(),
                fixture.inviteeProfile().getId()
        ));
        assertTrue(userTournamentRoleRepository.findRolesByUserIdAndTournamentId(
                fixture.inviteeUser().getId(),
                fixture.tournament().getId()
        ).contains(TournamentRole.VIEW));
    }

    @Test
    void rejectingInvitationRemovesPendingInviteWithoutMembershipOrRole() throws Exception {
        InvitationFixture fixture = invitationFixture();

        mockMvc.perform(post("/api/participant-invitations/{id}/reject", fixture.invitation().getId())
                        .servletPath("/api")
                        .with(authentication(authenticationFor(fixture.inviteeUser()))))
                .andExpect(status().isOk());

        assertTrue(participantInvitationRepository.findById(fixture.invitation().getId()).isEmpty());
        assertFalse(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(
                fixture.tournament().getId(),
                fixture.inviteeProfile().getId()
        ));
        assertTrue(userTournamentRoleRepository.findRolesByUserIdAndTournamentId(
                fixture.inviteeUser().getId(),
                fixture.tournament().getId()
        ).isEmpty());
    }

    private InvitationFixture invitationFixture() {
        ParticipantAccount inviter = participantAccount("inviter");
        ParticipantAccount invitee = participantAccount("invitee");
        ParticipantAccount other = participantAccount("other");
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());

        Team team = new Team();
        team.setName("Climate Cup team");
        team.setTournament(tournament);
        team.setActive(false);
        team.setCheckedIn(false);
        team.setDisqualified(false);
        team = teamRepository.saveAndFlush(team);

        TournamentParticipant inviterMember = new TournamentParticipant();
        inviterMember.setTeam(team);
        inviterMember.setParticipantProfile(inviter.profile());
        inviterMember.setSpeakerScore(0);
        tournamentParticipantRepository.saveAndFlush(inviterMember);

        ParticipantInvitation invitation = new ParticipantInvitation();
        invitation.setInviter(inviter.profile());
        invitation.setInvitee(invitee.profile());
        invitation.setTeam(team);
        invitation.setTimestamp(LocalDateTime.now());
        invitation.setAccepted(false);
        invitation = participantInvitationRepository.saveAndFlush(invitation);

        return new InvitationFixture(
                invitation,
                tournament,
                inviter.user(),
                invitee.user(),
                invitee.profile(),
                other.user()
        );
    }

    private ParticipantAccount participantAccount(String prefix) {
        String username = prefix + "-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "Participant",
                Role.PARTICIPANT
        ));
        ParticipantProfile profile = new ParticipantProfile();
        profile.setUser(user);
        profile = participantProfileRepository.saveAndFlush(profile);
        user.setProfile(profile);
        return new ParticipantAccount(user, profile);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Climate Cup");
        tournament.setDescription("Invitation contract fixture");
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

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("userTournamentPermissions");
        }
    }

    private record ParticipantAccount(User user, ParticipantProfile profile) {
    }

    private record InvitationFixture(
            ParticipantInvitation invitation,
            Tournament tournament,
            User inviterUser,
            User inviteeUser,
            ParticipantProfile inviteeProfile,
            User otherUser
    ) {
    }
}
