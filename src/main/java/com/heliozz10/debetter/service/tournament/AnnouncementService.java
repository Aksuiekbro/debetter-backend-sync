package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.tournament.announcement.Comment;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.announcement.AnnouncementRepository;
import com.heliozz10.debetter.repository.tournament.announcement.CommentRepository;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class AnnouncementService {
    private final EntityManager entityManager;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    private final TournamentRepository tournamentRepository;
    private final OrganizerProfileRepository organizerProfileRepository;

    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsByTournamentId(Long tournamentId, Pageable pageable) {
        return announcementRepository.findByTournamentId(tournamentId, pageable);
    }

    @Transactional(readOnly = true)
    public Announcement getAnnouncementByTournamentIdAndId(Long tournamentId, Long announcementId) {
        return announcementRepository.findByTournamentIdAndId(tournamentId, announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));
    }

    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsByTournamentIdAndAuthorId(Long tournamentId, Long authorId, Pageable pageable) {
        return announcementRepository.findByTournamentIdAndAuthorId(tournamentId, authorId, pageable);
    }

    @Transactional
    public Announcement addAnnouncementToTournament(AnnouncementFormDto announcementFormDto, Long tournamentId, Long creatorId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        Announcement announcement = announcementMapper.toAnnouncement(announcementFormDto);

        OrganizerProfile author = organizerProfileRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found"));

        announcement.setTournament(tournament);
        tournament.getAnnouncements().add(announcement);
        author.getAnnouncements().add(announcement);
        announcement.setAuthor(author);
        announcement.setTimestamp(LocalDateTime.now());
        announcement.setHidden(false);

        return announcementRepository.save(announcement);
    }

    /**
     * This helper method is used to update the lastEdited and lastEditor fields of the announcement
     * @param announcementId
     * @param editorId
     * @return the updated announcement
     */
    @Transactional
    private Announcement updateLastEdited(Long announcementId, Long editorId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        OrganizerProfile editor = entityManager.getReference(OrganizerProfile.class, editorId);

        announcement.setLastEdited(LocalDateTime.now());
        announcement.setLastEditor(editor);

        return announcement;
    }

    @Transactional
    public Announcement updateAnnouncement(AnnouncementFormDto announcementFormDto, Long tournamentId, Long announcementId, Long editorId) {
        Announcement announcement = updateLastEdited(announcementId, editorId);

        if(announcement.getTournament().getId() != tournamentId) {
            throw new EntityNotFoundException("Announcement not found");
        }

        announcementMapper.updateAnnouncement(announcementFormDto, announcement);

        return announcementRepository.save(announcement);
    }

    @Transactional
    public void hideAnnouncement(Long announcementId, Long editorId) {
        Announcement announcement = updateLastEdited(announcementId, editorId);

        announcement.setHidden(true);
    }

    @Transactional
    public void unhideAnnouncement(Long announcementId, Long editorId) {
        Announcement announcement = updateLastEdited(announcementId, editorId);

        announcement.setHidden(false);
    }

    @Transactional(readOnly = true)
    public List<Comment> getAnnouncementComments(Long tournamentId, Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        if(!Objects.equals(announcement.getTournament().getId(), tournamentId)) {
            throw new EntityNotFoundException("Announcement not found");
        }

        return announcement.getComments();
    }

    @Transactional
    public void addCommentToAnnouncement(String content, Long announcementId, Long authorId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        User author = entityManager.getReference(User.class, authorId);

        Comment comment = new Comment(content, LocalDateTime.now(), announcement, author);

        announcement.getComments().add(comment);

        commentRepository.save(comment);
    }

    @Transactional
    public void removeCommentFromAnnouncement(Long authorId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if(!Objects.equals(comment.getAuthor().getId(), authorId)) {
            throw new EntityNotFoundException("Comment not found");
        }

        comment.getAnnouncement().getComments().removeIf(c -> Objects.equals(c.getId(), commentId));
    }

    @Transactional
    public void removeAnnouncementFromTournament(Long announcementId, Long tournamentId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        if(!Objects.equals(announcement.getTournament().getId(), tournamentId)) {
            throw new EntityNotFoundException("Announcement not found");
        }

        announcementRepository.deleteById(announcementId);
    }
}
