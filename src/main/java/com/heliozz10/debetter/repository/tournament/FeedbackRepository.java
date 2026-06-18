package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    @EntityGraph(value = "Feedback.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<Feedback> findAll(Specification<Feedback> spec, Pageable pageable);

    List<Feedback> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<Feedback> findByTournamentId(Long tournamentId);
    List<Feedback> findByAuthorId(Long authorId);

    @EntityGraph(value = "Feedback.forView", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Feedback> findByTournamentIdAndId(Long tournamentId, Long id);
    Optional<Feedback> findByAuthorIdAndId(Long authorId, Long id);
}
