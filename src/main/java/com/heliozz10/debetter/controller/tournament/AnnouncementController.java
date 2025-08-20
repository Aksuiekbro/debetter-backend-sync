package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.dto.tournament.announcement.out.AnnouncementView;
import com.heliozz10.debetter.dto.tournament.announcement.out.CommentView;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.tournament.announcement.CommentMapper;
import com.heliozz10.debetter.service.tournament.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
                announcementMapper.toAnnouncementViews(announcements.getContent()),
                announcements.getTotalElements(),
                announcements.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public AnnouncementView getAnnouncementById(@PathVariable Long tournamentId, @PathVariable Long id) {
        return announcementMapper.toAnnouncementView(announcementService.getAnnouncementByTournamentIdAndId(tournamentId, id));
    }

    @PostMapping
    public AnnouncementView addAnnouncement(@PathVariable Long tournamentId, @RequestBody AnnouncementFormDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return announcementMapper.toAnnouncementView(announcementService.addAnnouncementToTournament(dto, tournamentId, profile.getId()));
    }

    @PatchMapping("/{id}")
    public AnnouncementView updateAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id, @RequestBody AnnouncementFormDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return announcementMapper.toAnnouncementView(announcementService.updateAnnouncement(dto, tournamentId, id, profile.getId()));
    }

    @DeleteMapping("/{id}")
    public void removeAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id) {
        announcementService.removeAnnouncementFromTournament(tournamentId, id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentView> getAnnouncementComments(@PathVariable Long tournamentId, @PathVariable Long id) {
        return commentMapper.toCommentViews(announcementService.getAnnouncementComments(tournamentId, id));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public void removeCommentFromAnnouncement(@PathVariable Long commentId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        announcementService.removeCommentFromAnnouncement(profile.getId(), commentId);
    }
}
