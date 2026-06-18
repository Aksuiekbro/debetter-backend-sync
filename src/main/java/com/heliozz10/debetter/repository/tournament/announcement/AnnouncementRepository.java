package com.heliozz10.debetter.repository.tournament.announcement;

import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(value = "Announcement.forView", type = EntityGraph.EntityGraphType.LOAD)
    Page<Announcement> findByTournamentId(Long tournamentId, Pageable pageable);

    @EntityGraph(value = "Announcement.forView", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Announcement> findByTournamentIdAndId(Long tournamentId, Long id);

    Page<Announcement> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(value = "Announcement.forView", type = EntityGraph.EntityGraphType.LOAD)
    Page<Announcement> findByTournamentIdAndAuthorId(Long tournamentId, Long authorId, Pageable pageable);
}
