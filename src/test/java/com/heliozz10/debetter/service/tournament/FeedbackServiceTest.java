package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import com.heliozz10.debetter.dto.tournament.in.FeedbackDto;
import com.heliozz10.debetter.mapper.tournament.FeedbackMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.tournament.FeedbackRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    @Mock
    private EntityManager entityManager;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackMapper feedbackMapper;
    @Mock
    private UserMapper userMapper;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(entityManager, feedbackRepository, feedbackMapper, userMapper);
    }

    @Test
    void updateBindsTournamentAuthorAndFeedbackIds() {
        FeedbackDto dto = new FeedbackDto("Updated", "Updated feedback");
        Feedback feedback = new Feedback();
        when(feedbackRepository.findByTournamentIdAndAuthorIdAndId(53L, 7L, 11L))
                .thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(feedback)).thenReturn(feedback);

        Feedback updated = feedbackService.updateFeedback(dto, 53L, 11L, 7L);

        assertSame(feedback, updated);
        verify(feedbackRepository).findByTournamentIdAndAuthorIdAndId(53L, 7L, 11L);
        verify(feedbackMapper).updateFeedback(dto, feedback);
    }

    @Test
    void deleteRejectsAFeedbackIdFromAnotherTournament() {
        when(feedbackRepository.findByTournamentIdAndAuthorIdAndId(53L, 7L, 11L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> feedbackService.deleteFeedback(53L, 11L, 7L)
        );

        verify(feedbackRepository).findByTournamentIdAndAuthorIdAndId(53L, 7L, 11L);
        verify(feedbackRepository, never()).delete(org.mockito.ArgumentMatchers.any(Feedback.class));
    }
}
