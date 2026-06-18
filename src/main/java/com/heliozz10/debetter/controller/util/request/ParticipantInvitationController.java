package com.heliozz10.debetter.controller.util.request;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.util.request.in.ParticipantInvitationDto;
import com.heliozz10.debetter.dto.util.request.out.ParticipantInvitationView;
import com.heliozz10.debetter.mapper.util.request.ParticipantInvitationMapper;
import com.heliozz10.debetter.service.util.request.ParticipantInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RequiredArgsConstructor
@RestController
@PreAuthorize("principal.role.name() == 'PARTICIPANT'")
@RequestMapping("/participant-invitations")
public class ParticipantInvitationController {
    private final ParticipantInvitationService participantInvitationService;
    private final ParticipantInvitationMapper participantInvitationMapper;

    @GetMapping("/sent")
    public PageableResult<ParticipantInvitationView> getSentParticipantInvitations(
            Authentication authentication,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        Page<ParticipantInvitation> invitations = participantInvitationService.getInvitationsByInviterId(profile.getId(), pageable);
        return new PageableResult<>(
                invitations.getContent().stream().map(participantInvitationService::toParticipantInvitationView).toList(),
                invitations.getTotalElements(),
                invitations.getTotalPages()
        );
    }

    @GetMapping("/received")
    public PageableResult<ParticipantInvitationView> getReceivedParticipantInvitations(
            Authentication authentication,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        Page<ParticipantInvitation> invitations = participantInvitationService.getInvitationsByInviteeId(profile.getId(), pageable);
        return new PageableResult<>(
                invitations.getContent().stream().map(participantInvitationService::toParticipantInvitationView).toList(),
                invitations.getTotalElements(),
                invitations.getTotalPages()
        );
    }

    @PostMapping
    public ParticipantInvitationView createParticipantInvitation(@Valid @RequestBody ParticipantInvitationDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if(user == null) {
            return null;
        }
        if(Objects.equals(dto.inviteeUsername(), user.getUsername())) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();

        //TODO: fix security
        return participantInvitationMapper.toParticipantInvitationView(participantInvitationService.createInvitation(profile.getId(), dto.inviteeUsername(), dto.teamId()));
    }

    @PostMapping("/{id}/accept")
    public void acceptInvitation(@PathVariable Long id, Authentication authentication) {
        Long inviteeId = ((User) authentication.getPrincipal()).getProfile().getId();
        participantInvitationService.acceptInvitation(id, inviteeId);
    }

    @PostMapping("/{id}/reject")
    public void rejectInvitation(@PathVariable Long id, Authentication authentication) {
        Long inviteeId = ((User) authentication.getPrincipal()).getProfile().getId();
        participantInvitationService.rejectInvitation(id, inviteeId);
    }
}
