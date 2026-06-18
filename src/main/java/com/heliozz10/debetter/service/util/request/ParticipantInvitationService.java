package com.heliozz10.debetter.service.util.request;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.dto.util.request.out.ParticipantInvitationView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.util.request.ParticipantInvitationMapper;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.repository.util.request.ParticipantInvitationRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
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
    private final ParticipantInvitationMapper participantInvitationMapper;

    private final TeamRepository teamRepository;
    private final TeamService teamService;

    private final TournamentParticipantRepository tournamentParticipantRepository;

    private final TournamentSecurity tournamentSecurity;

    private final UserMapper userMapper;
    private final ParticipantProfileRepository participantProfileRepository;

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
    private ParticipantInvitation buildInvitation(Long inviterId, String inviteeUsername, Team team) {
        ParticipantProfile inviter = entityManager.getReference(ParticipantProfile.class, inviterId);
        ParticipantProfile invitee = participantProfileRepository.findByUser_Username(inviteeUsername)
                .orElseThrow(() -> new EntityNotFoundException("Invitee not found"));

        ParticipantInvitation invitation = new ParticipantInvitation();
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setTeam(team);
        invitation.setTimestamp(LocalDateTime.now());
        invitation.setAccepted(false);

        return invitation;
    }

    @Transactional
    public ParticipantInvitation createInvitation(Long inviterId, String inviteeUsername, Long teamId) {
        long existingInvitationCount = participantInvitationRepository.countExistingInvitations(inviterId, inviteeUsername, teamId);

        if (existingInvitationCount > 0) {
            throw new IllegalArgumentException("Invitation already exists");
        }

        Team team = teamRepository.findFullById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if(!team.getMembers().stream().anyMatch(member -> member.getParticipantProfile().getId().equals(inviterId))) {
            throw new IllegalArgumentException("Inviter is not a member of the team");
        }

        teamService.validateTeamSize(team);

        ParticipantInvitation invitation = buildInvitation(inviterId, inviteeUsername, team);
        return participantInvitationRepository.save(invitation);
    }

    /**
     * Should only be used for tournament creation initial invitations
     * @param inviterId
     * @param inviteeUsernames
     * @param teamId
     * @return
     */
    @Transactional
    public List<ParticipantInvitation> createInvitations(Long inviterId, Collection<String> inviteeUsernames, Long teamId) {
        Team team = teamRepository.findFullById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        List<ParticipantInvitation> invitations = inviteeUsernames.stream()
                .map(inviteeUsername -> buildInvitation(inviterId, inviteeUsername, team))
                .toList();

        return participantInvitationRepository.saveAll(invitations);
    }

    @Transactional
    public void acceptInvitation(Long invitationId, Long inviteeId) {
        ParticipantInvitation invitation = participantInvitationRepository.findByInviteeIdAndId(inviteeId, invitationId)
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
        participant.setSpeakerScore(0);

        tournamentParticipantRepository.save(participant);

        tournamentSecurity.assignRoleToUser(invitation.getInvitee().getUser().getId(), team.getTournament().getId(), TournamentRole.VIEW);
    }

    @Transactional
    public void rejectInvitation(Long invitationId, Long inviteeId) {
        deleteInvitation(invitationId, inviteeId);
    }

    @Transactional
    public void deleteInvitation(Long invitationId, Long inviteeId) {
        ParticipantInvitation invitation = participantInvitationRepository.findRawByInviteeIdAndId(inviteeId, invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        participantInvitationRepository.deleteById(invitationId);
    }

    public ParticipantInvitationView toParticipantInvitationView(ParticipantInvitation invitation) {
        ParticipantInvitationView view = participantInvitationMapper.toParticipantInvitationView(invitation);
        view.setInviter(userMapper.toSimpleUserView(invitation.getInviter().getUser()));
        view.setInvitee(userMapper.toSimpleUserView(invitation.getInvitee().getUser()));
        return view;
    }
}
