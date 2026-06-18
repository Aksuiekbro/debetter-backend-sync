package com.heliozz10.debetter.controller.util.request;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.util.request.in.OrganizerInvitationDto;
import com.heliozz10.debetter.dto.util.request.out.OrganizerInvitationView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.util.request.OrganizerInvitationMapper;
import com.heliozz10.debetter.service.util.request.OrganizerInvitationService;
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
@PreAuthorize("principal.role.name() == 'ORGANIZER'")
@RequestMapping("/organizer-invitations")
public class OrganizerInvitationController {
    private final OrganizerInvitationService organizerInvitationService;
    private final OrganizerInvitationMapper organizerInvitationMapper;

    private final UserMapper userMapper;

    @GetMapping("/sent")
    public PageableResult<OrganizerInvitationView> getSentOrganizerInvitations(
            Authentication authentication,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        Page<OrganizerInvitation> invitations = organizerInvitationService.getInvitationsByInviterId(profile.getId(), pageable);
        return new PageableResult<>(
                invitations.getContent().stream().map(organizerInvitationService::toOrganizerInvitationView).toList(),
                invitations.getTotalElements(),
                invitations.getTotalPages()
        );
    }

    @GetMapping("/received")
    public PageableResult<OrganizerInvitationView> getReceivedOrganizerInvitations(
            Authentication authentication,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        Page<OrganizerInvitation> invitations = organizerInvitationService.getInvitationsByInviteeId(profile.getId(), pageable);
        return new PageableResult<>(
                invitations.getContent().stream().map(organizerInvitationService::toOrganizerInvitationView).toList(),
                invitations.getTotalElements(),
                invitations.getTotalPages()
        );
    }

    @PostMapping
    public OrganizerInvitationView createOrganizerInvitation(@Valid @RequestBody OrganizerInvitationDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if(user == null) {
            return null;
        }
        if(Objects.equals(dto.inviteeUsername(), user.getUsername())) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return organizerInvitationMapper.toOrganizerInvitationView(organizerInvitationService.createInvitation(profile.getId(), dto.inviteeUsername(), dto.tournamentId()));
    }

    @PostMapping("/{id}/accept")
    public void acceptInvitation(@PathVariable Long id, Authentication authentication) {
        Long inviteeId = ((User) authentication.getPrincipal()).getProfile().getId();
        organizerInvitationService.acceptInvitation(id, inviteeId);
    }

    @PostMapping("/{id}/reject")
    public void rejectInvitation(@PathVariable Long id, Authentication authentication) {
        Long inviteeId = ((User) authentication.getPrincipal()).getProfile().getId();
        organizerInvitationService.rejectInvitation(id, inviteeId);
    }
}
