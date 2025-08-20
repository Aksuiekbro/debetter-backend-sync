package com.heliozz10.debetter.controller.util.request;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.util.request.in.ParticipantInvitationDto;
import com.heliozz10.debetter.dto.util.request.out.ParticipantInvitationView;
import com.heliozz10.debetter.mapper.util.request.ParticipantInvitationMapper;
import com.heliozz10.debetter.service.util.request.ParticipantInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
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
                participantInvitationMapper.toParticipantInvitationViews(invitations.getContent()),
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
                participantInvitationMapper.toParticipantInvitationViews(invitations.getContent()),
                invitations.getTotalElements(),
                invitations.getTotalPages()
        );
    }

    @PostMapping
    public ParticipantInvitationView createParticipantInvitation(@RequestBody ParticipantInvitationDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        return participantInvitationMapper.toParticipantInvitationView(participantInvitationService.createInvitation(profile.getId(), dto.inviteeId(), dto.teamId()));
    }

    @PutMapping("/{id}/accept")
    public void acceptInvitation(@PathVariable Long id) {
        participantInvitationService.acceptInvitation(id);
    }

    @PutMapping("/{id}/reject")
    public void rejectInvitation(@PathVariable Long id) {
        participantInvitationService.rejectInvitation(id);
    }
}
