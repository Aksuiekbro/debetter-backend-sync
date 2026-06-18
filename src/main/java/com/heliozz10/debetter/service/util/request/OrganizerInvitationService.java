package com.heliozz10.debetter.service.util.request;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.util.request.out.OrganizerInvitationView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.util.request.OrganizerInvitationMapper;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.repository.util.request.OrganizerInvitationRepository;
import com.heliozz10.debetter.service.tournament.TournamentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class OrganizerInvitationService {
    private final EntityManager entityManager;

    private final OrganizerInvitationRepository organizerInvitationRepository;
    private final OrganizerInvitationMapper organizerInvitationMapper;

    private final TournamentService tournamentService;

    private final UserMapper userMapper;
    private final OrganizerProfileRepository organizerProfileRepository;

    @Transactional(readOnly = true)
    public Page<OrganizerInvitation> getInvitationsByInviteeId(Long inviteeId, Pageable pageable) {
        return organizerInvitationRepository.findByInviteeId(inviteeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrganizerInvitation> getInvitationsByInviterId(Long inviterId, Pageable pageable) {
        return organizerInvitationRepository.findByInviterId(inviterId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrganizerInvitation> getInvitationsByTournamentId(Long tournamentId, Pageable pageable) {
        return organizerInvitationRepository.findByTournamentId(tournamentId, pageable);
    }

    @Transactional
    public OrganizerInvitation createInvitation(Long inviterId, String inviteeUsername, Long tournamentId) {
        long existingInvitationCount = organizerInvitationRepository.countExistingInvitations(inviterId, inviteeUsername, tournamentId);

        if (existingInvitationCount > 0) {
            throw new IllegalArgumentException("Invitation already exists");
        }

        OrganizerInvitation invitation = new OrganizerInvitation();

        OrganizerProfile inviter = entityManager.getReference(OrganizerProfile.class, inviterId);
        OrganizerProfile invitee = organizerProfileRepository.findByUser_Username(inviteeUsername)
                .orElseThrow(() -> new EntityNotFoundException("Invitee not found"));
        Tournament tournament = entityManager.getReference(Tournament.class, tournamentId);

        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setTournament(tournament);

        invitation.setTimestamp(LocalDateTime.now());
        invitation.setAccepted(false);

        return organizerInvitationRepository.save(invitation);
    }

    @Transactional
    public void acceptInvitation(Long invitationId, Long inviteeId) {
        OrganizerInvitation invitation = organizerInvitationRepository.findByInviteeIdAndId(invitationId, inviteeId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        invitation.setAccepted(true);

        tournamentService.addOrganizerToTournament(invitation.getInvitee().getId(), invitation.getTournament().getId());
    }

    @Transactional
    public void rejectInvitation(Long invitationId, Long inviteeId) {
        deleteInvitation(invitationId, inviteeId);
    }

    @Transactional
    public void deleteInvitation(Long invitationId, Long inviteeId) {
        OrganizerInvitation invitation = organizerInvitationRepository.findRawByInviteeIdAndId(inviteeId, invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        organizerInvitationRepository.deleteById(invitationId);
    }

    public OrganizerInvitationView toOrganizerInvitationView(OrganizerInvitation invitation) {
        OrganizerInvitationView view = organizerInvitationMapper.toOrganizerInvitationView(invitation);
        view.setInviter(userMapper.toSimpleUserView(invitation.getInviter().getUser()));
        view.setInvitee(userMapper.toSimpleUserView(invitation.getInvitee().getUser()));
        return view;
    }
}
