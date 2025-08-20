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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/feedbacks")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    //TODO: special security case
    @GetMapping
    public PageableResult<FeedbackView> getFeedbacks(
            @PathVariable Long tournamentId,
            @ModelAttribute FeedbackGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Feedback> feedbacks = feedbackService.getFeedbacks(tournamentId, params, pageable);
        return new PageableResult<>(
                feedbackMapper.toFeedbackViews(feedbacks.getContent()),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public FeedbackView getFeedbackById(@PathVariable Long tournamentId, @PathVariable Long id) {
        return feedbackMapper.toFeedbackView(feedbackService.getFeedbackByTournamentIdAndId(tournamentId, id));
    }

    @PostMapping
    public FeedbackView addFeedback(@PathVariable Long tournamentId, @RequestBody FeedbackDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        return feedbackMapper.toFeedbackView(feedbackService.addFeedbackToTournament(dto, tournamentId, profile.getId()));
    }

    @PatchMapping("/{id}")
    public FeedbackView updateFeedback(@PathVariable Long tournamentId, @PathVariable Long id, @RequestBody FeedbackDto dto) {
        return feedbackMapper.toFeedbackView(feedbackService.updateFeedback(dto, id));
    }

    @DeleteMapping("/{id}")
    public void deleteFeedback(@PathVariable Long tournamentId, @PathVariable Long id) {
        feedbackService.deleteFeedback(id);
    }
}
