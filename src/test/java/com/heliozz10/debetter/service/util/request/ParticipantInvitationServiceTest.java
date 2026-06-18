package com.heliozz10.debetter.service.util.request;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.util.request.ParticipantInvitationMapper;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.repository.util.request.ParticipantInvitationRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import com.heliozz10.debetter.service.tournament.TeamService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantInvitationServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private ParticipantInvitationRepository participantInvitationRepository;

    @Mock
    private ParticipantInvitationMapper participantInvitationMapper;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamService teamService;

    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Mock
    private TournamentSecurity tournamentSecurity;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ParticipantProfileRepository participantProfileRepository;

    private ParticipantInvitationService participantInvitationService;

    @BeforeEach
    void setUp() {
        participantInvitationService = new ParticipantInvitationService(
                entityManager,
                participantInvitationRepository,
                participantInvitationMapper,
                teamRepository,
                teamService,
                tournamentParticipantRepository,
                tournamentSecurity,
                userMapper,
                participantProfileRepository
        );
    }

    @Test
    void createInvitationRecognizesTeamMemberByParticipantProfileId() {
        ParticipantProfile inviter = participantProfile(10L, 99L, "arman");
        ParticipantProfile invitee = participantProfile(11L, 100L, "aisha");
        TournamentParticipant member = new TournamentParticipant();
        member.setParticipantProfile(inviter);

        Team team = new Team();
        team.setMembers(new ArrayList<>(List.of(member)));

        when(participantInvitationRepository.countExistingInvitations(10L, "aisha", 55L)).thenReturn(0L);
        when(teamRepository.findFullById(55L)).thenReturn(Optional.of(team));
        when(entityManager.getReference(ParticipantProfile.class, 10L)).thenReturn(inviter);
        when(participantProfileRepository.findByUser_Username("aisha")).thenReturn(Optional.of(invitee));
        when(participantInvitationRepository.save(any(ParticipantInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantInvitation invitation = participantInvitationService.createInvitation(10L, "aisha", 55L);

        assertSame(inviter, invitation.getInviter());
        assertSame(invitee, invitation.getInvitee());
        assertSame(team, invitation.getTeam());
        verify(teamService).validateTeamSize(team);
    }

    private ParticipantProfile participantProfile(Long profileId, Long userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        ParticipantProfile profile = new ParticipantProfile();
        profile.setId(profileId);
        profile.setUser(user);
        return profile;
    }
}
