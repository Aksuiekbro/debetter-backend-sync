package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.dto.tournament.announcement.in.CommentDto;
import com.heliozz10.debetter.dto.tournament.announcement.out.AnnouncementView;
import com.heliozz10.debetter.dto.tournament.announcement.out.CommentView;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.tournament.announcement.CommentMapper;
import com.heliozz10.debetter.service.tournament.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("tournaments/{tournamentId}/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final AnnouncementMapper announcementMapper;
    private final CommentMapper commentMapper;

    @GetMapping
    public PageableResult<AnnouncementView> getAnnouncementsByTournamentId(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long authorId,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Announcement> announcements = authorId == null ?
                announcementService.getAnnouncementsByTournamentId(tournamentId, pageable) :
                announcementService.getAnnouncementsByTournamentIdAndAuthorId(tournamentId, authorId, pageable);
        return new PageableResult<>(
                announcements.getContent().stream().map(announcementService::toAnnouncementView).toList(),
                announcements.getTotalElements(),
                announcements.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public AnnouncementView getAnnouncementById(@PathVariable Long tournamentId, @PathVariable Long id) {
        return announcementService.toAnnouncementView(announcementService.getAnnouncementByTournamentIdAndId(tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PostMapping
    public AnnouncementView addAnnouncement(@PathVariable Long tournamentId, @Valid @RequestPart("data") AnnouncementFormDto dto, @RequestPart(value = "image", required = false) MultipartFile image, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return announcementService.toAnnouncementView(announcementService.addAnnouncementToTournament(dto, image, tournamentId, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{id}")
    public AnnouncementView updateAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestPart("data") AnnouncementFormDto dto, @RequestPart(value = "image", required = false) MultipartFile image, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return announcementService.toAnnouncementView(announcementService.updateAnnouncement(dto, image, tournamentId, id, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping("/{id}")
    public void removeAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id) {
        announcementService.removeAnnouncementFromTournament(tournamentId, id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentView> getAnnouncementComments(@PathVariable Long tournamentId, @PathVariable Long id) {
        return commentMapper.toCommentViews(announcementService.getAnnouncementComments(tournamentId, id));
    }

    @PostMapping("/{id}/comments")
    public void addCommentToAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody CommentDto dto, Authentication authentication) {
        Long authorId = ((User) authentication.getPrincipal()).getId();
        announcementService.addCommentToAnnouncement(tournamentId, id, authorId, dto);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public void removeCommentFromAnnouncement(@PathVariable Long commentId, Authentication authentication) {
        Long authorId = ((User) authentication.getPrincipal()).getId();
        announcementService.removeCommentFromAnnouncement(authorId, commentId);
    }
}
