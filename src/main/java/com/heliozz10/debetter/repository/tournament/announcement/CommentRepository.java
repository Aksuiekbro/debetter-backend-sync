package com.heliozz10.debetter.repository.tournament.announcement;

import com.heliozz10.debetter.content.tournament.announcement.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByAnnouncementId(Long announcementId);

    Optional<Comment> findByAuthorIdAndId(Long authorId, Long id);

    @Query("""
            select c from Comment c
            where c.author.id = :authorId
              and c.announcement.tournament.id = :tournamentId
              and c.announcement.id = :announcementId
              and c.id = :commentId
            """)
    Optional<Comment> findOwnedComment(
            @Param("tournamentId") Long tournamentId,
            @Param("announcementId") Long announcementId,
            @Param("authorId") Long authorId,
            @Param("commentId") Long commentId
    );

    List<Comment> findByAuthorId(Long authorId);
    List<Comment> findByAnnouncementIdAndTimestampBetween(Long announcementId, LocalDateTime start, LocalDateTime end);
}
