package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.tournament.in.FeedbackDto;
import com.heliozz10.debetter.dto.tournament.in.FeedbackGetParams;
import com.heliozz10.debetter.mapper.tournament.FeedbackMapper;
import com.heliozz10.debetter.repository.specification.tournament.FeedbackSpecification;
import com.heliozz10.debetter.repository.tournament.FeedbackRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class FeedbackService {
    private final EntityManager entityManager;

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbacks(Long tournamentId, FeedbackGetParams params, Pageable pageable) {
        //TODO: validate tournament id exists
        Specification<Feedback> spec = FeedbackSpecification.filterBy(tournamentId, params, entityManager);
        return feedbackRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Feedback getFeedbackByTournamentIdAndId(Long tournamentId, Long id) {
        return feedbackRepository.findByTournamentIdAndId(tournamentId, id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found"));
    }

    @Transactional
    public Feedback addFeedbackToTournament(FeedbackDto dto, Long tournamentId, Long authorId) {
        Feedback feedback = feedbackMapper.toFeedback(dto);

        Tournament tournament = entityManager.getReference(Tournament.class, tournamentId);
        ParticipantProfile author = entityManager.getReference(ParticipantProfile.class, authorId);

        feedback.setTournament(tournament);
        feedback.setAuthor(author);
        feedback.setTimestamp(LocalDateTime.now());

        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback updateFeedback(FeedbackDto dto, Long feedbackId) {
        Feedback feedback = entityManager.getReference(Feedback.class, feedbackId);

        feedbackMapper.updateFeedback(dto, feedback);
        feedback.setEdited(true);

        return feedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Long feedbackId) {
        Feedback feedback = entityManager.getReference(Feedback.class, feedbackId);
        feedbackRepository.delete(feedback);
    }
}
