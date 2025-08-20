package com.heliozz10.debetter.repository.tournament.announcement;

import com.heliozz10.debetter.content.tournament.announcement.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByAnnouncementId(Long announcementId);
    List<Comment> findByAuthorId(Long authorId);
    List<Comment> findByAnnouncementIdAndTimestampBetween(Long announcementId, LocalDateTime start, LocalDateTime end);
}
