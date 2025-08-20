package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    List<Feedback> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<Feedback> findByTournamentId(Long tournamentId);
    List<Feedback> findByAuthorId(Long authorId);

    Optional<Feedback> findByTournamentIdAndId(Long tournamentId, Long id);
}
