package com.heliozz10.debetter.service.util.request;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.util.request.ParticipantInvitationRepository;
import com.heliozz10.debetter.service.tournament.TeamService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ParticipantInvitationService {
    private final EntityManager entityManager;

    private final ParticipantInvitationRepository participantInvitationRepository;

    private final TeamRepository teamRepository;
    private final TeamService teamService;

    private final TournamentParticipantRepository tournamentParticipantRepository;

    @Transactional(readOnly = true)
    public Page<ParticipantInvitation> getInvitationsByInviteeId(Long inviteeId, Pageable pageable) {
        return participantInvitationRepository.findByInviteeId(inviteeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ParticipantInvitation> getInvitationsByInviterId(Long inviterId, Pageable pageable) {
        return participantInvitationRepository.findByInviterId(inviterId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ParticipantInvitation> getInvitationsByTeamId(Long teamId, Pageable pageable) {
        return participantInvitationRepository.findByTeamId(teamId, pageable);
    }

    /**
     * Creates a participant invitation entity (not persisted).
     */
    private ParticipantInvitation buildInvitation(Long inviterId, Long inviteeId, Team team) {
        ParticipantProfile inviter = entityManager.getReference(ParticipantProfile.class, inviterId);
        ParticipantProfile invitee = entityManager.getReference(ParticipantProfile.class, inviteeId);

        ParticipantInvitation invitation = new ParticipantInvitation();
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setTeam(team);
        invitation.setTimestamp(LocalDateTime.now());
        invitation.setAccepted(false);

        return invitation;
    }

    @Transactional
    public ParticipantInvitation createInvitation(Long inviterId, Long inviteeId, Long teamId) {
        long existingInvitationCount = participantInvitationRepository.countExistingInvitation(inviterId, inviteeId, teamId);

        if (existingInvitationCount > 0) {
            throw new IllegalArgumentException("Invitation already exists");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        teamService.validateTeamSize(team);

        ParticipantInvitation invitation = buildInvitation(inviterId, inviteeId, team);
        return participantInvitationRepository.save(invitation);
    }

    /**
     * Should only be used for tournament creation initial invitations
     * @param inviterId
     * @param inviteeIds
     * @param teamId
     * @return
     */
    @Transactional
    public List<ParticipantInvitation> createInvitations(Long inviterId, Collection<Long> inviteeIds, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        teamService.validateTeamSize(team);

        List<ParticipantInvitation> invitations = inviteeIds.stream()
                .map(inviteeId -> buildInvitation(inviterId, inviteeId, team))
                .toList();

        return participantInvitationRepository.saveAll(invitations);
    }

    @Transactional
    public void acceptInvitation(Long invitationId) {
        ParticipantInvitation invitation = participantInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        invitation.setAccepted(true);

        Team team = invitation.getTeam();

        int newSize = team.getMembers().size() + 1;
        int maxSize = team.getTournament().getPreliminaryFormat() == DebateFormat.KP ? 3 : 2;

        if (newSize > maxSize) {
            throw new IllegalArgumentException("Team is full");
        } else if (newSize == maxSize) {
            team.setActive(true);
        }

        TournamentParticipant participant = new TournamentParticipant();
        participant.setTeam(team);
        participant.setParticipantProfile(invitation.getInvitee());

        tournamentParticipantRepository.save(participant);
    }

    @Transactional
    public void rejectInvitation(Long invitationId) {
        deleteInvitation(invitationId);
    }

    @Transactional
    public void deleteInvitation(Long invitationId) {
        participantInvitationRepository.deleteById(invitationId);
    }
}
