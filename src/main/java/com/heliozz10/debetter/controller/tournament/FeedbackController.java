package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.FeedbackDto;
import com.heliozz10.debetter.dto.tournament.in.FeedbackGetParams;
import com.heliozz10.debetter.dto.tournament.out.FeedbackView;
import com.heliozz10.debetter.mapper.tournament.FeedbackMapper;
import com.heliozz10.debetter.service.tournament.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/feedbacks")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    @GetMapping
    public PageableResult<FeedbackView> getFeedbacks(
            @PathVariable Long tournamentId,
            @Valid @ModelAttribute FeedbackGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Feedback> feedbacks = feedbackService.getFeedbacks(tournamentId, params, pageable);
        return new PageableResult<>(
                feedbacks.getContent().stream().map(feedbackService::toFeedbackView).toList(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public FeedbackView getFeedbackById(@PathVariable Long tournamentId, @PathVariable Long id) {
        return feedbackService.toFeedbackView(feedbackService.getFeedbackByTournamentIdAndId(tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'PARTICIPANT' and @tournamentSecurity.hasViewPermission(principal, #tournamentId)")
    @PostMapping
    public FeedbackView addFeedback(@PathVariable Long tournamentId, @Valid @RequestBody FeedbackDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        return feedbackService.toFeedbackView(feedbackService.addFeedbackToTournament(dto, tournamentId, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'PARTICIPANT' and @tournamentSecurity.hasViewPermission(principal, #tournamentId)")
    @PatchMapping("/{id}")
    public FeedbackView updateFeedback(Authentication authentication, @PathVariable Long id, @Valid @RequestBody FeedbackDto dto) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        return feedbackService.toFeedbackView(feedbackService.updateFeedback(dto, id, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'PARTICIPANT'")
    @DeleteMapping("/{id}")
    public void deleteFeedback(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        feedbackService.deleteFeedback(id, profile.getId());
    }
}
