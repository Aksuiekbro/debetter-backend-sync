package com.heliozz10.debetter.service.util.request;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.content.util.request.OrganizerInvitationStatus;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.util.request.OrganizerInvitationMapper;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.repository.util.request.OrganizerInvitationRepository;
import com.heliozz10.debetter.service.tournament.TournamentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerInvitationServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private OrganizerInvitationRepository organizerInvitationRepository;

    @Mock
    private OrganizerInvitationMapper organizerInvitationMapper;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrganizerProfileRepository organizerProfileRepository;

    private OrganizerInvitationService organizerInvitationService;

    @BeforeEach
    void setUp() {
        organizerInvitationService = new OrganizerInvitationService(
                entityManager,
                organizerInvitationRepository,
                organizerInvitationMapper,
                tournamentService,
                userMapper,
                organizerProfileRepository
        );
    }

    @Test
    void acceptLooksUpInvitationByInviteeThenInvitationIdAndGrantsEditAccess() {
        OrganizerInvitation invitation = invitation(91L, 23L, 47L);
        when(organizerInvitationRepository.findByInviteeIdAndId(23L, 91L))
                .thenReturn(Optional.of(invitation));

        organizerInvitationService.acceptInvitation(91L, 23L);

        verify(organizerInvitationRepository).findByInviteeIdAndId(23L, 91L);
        verify(tournamentService).addOrganizerToTournament(23L, 47L);
        assertTrue(invitation.getAccepted());
        assertEquals(OrganizerInvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    void rejectKeepsInvitationAsADeclinedStatusForRefreshes() {
        OrganizerInvitation invitation = invitation(91L, 23L, 47L);
        when(organizerInvitationRepository.findRawByInviteeIdAndId(23L, 91L))
                .thenReturn(Optional.of(invitation));

        organizerInvitationService.rejectInvitation(91L, 23L);

        assertNull(invitation.getAccepted());
        assertEquals(OrganizerInvitationStatus.DECLINED, invitation.getStatus());
        verify(organizerInvitationRepository, never()).deleteById(91L);
        verify(tournamentService, never()).addOrganizerToTournament(23L, 47L);
    }

    @Test
    void createInvitationReopensADeclinedInvitationAsPending() {
        OrganizerInvitation declined = invitation(91L, 23L, 47L);
        declined.setAccepted(null);
        LocalDateTime declinedAt = LocalDateTime.now().minusDays(1);
        declined.setTimestamp(declinedAt);
        when(organizerInvitationRepository.findExistingInvitation(17L, "invitee", 47L))
                .thenReturn(Optional.of(declined));
        when(organizerInvitationRepository.save(declined)).thenReturn(declined);

        OrganizerInvitation reopened = organizerInvitationService.createInvitation(17L, "invitee", 47L);

        assertSame(declined, reopened);
        assertEquals(OrganizerInvitationStatus.PENDING, reopened.getStatus());
        assertTrue(reopened.getTimestamp().isAfter(declinedAt));
        verify(organizerInvitationRepository).save(declined);
    }

    @Test
    void createInvitationRejectsAnExistingPendingOrAcceptedInvitation() {
        OrganizerInvitation pending = invitation(91L, 23L, 47L);
        when(organizerInvitationRepository.findExistingInvitation(17L, "invitee", 47L))
                .thenReturn(Optional.of(pending));

        assertThrows(
                IllegalArgumentException.class,
                () -> organizerInvitationService.createInvitation(17L, "invitee", 47L)
        );

        pending.setAccepted(true);
        assertThrows(
                IllegalArgumentException.class,
                () -> organizerInvitationService.createInvitation(17L, "invitee", 47L)
        );
        verify(organizerInvitationRepository, never()).save(pending);
    }

    private static OrganizerInvitation invitation(Long invitationId, Long inviteeId, Long tournamentId) {
        User inviteeUser = new User();
        inviteeUser.setId(230L);

        OrganizerProfile invitee = new OrganizerProfile();
        invitee.setId(inviteeId);
        invitee.setUser(inviteeUser);

        Tournament tournament = new Tournament();
        tournament.setId(tournamentId);

        OrganizerInvitation invitation = new OrganizerInvitation();
        invitation.setId(invitationId);
        invitation.setInvitee(invitee);
        invitation.setTournament(tournament);
        invitation.setAccepted(false);
        return invitation;
    }
}
