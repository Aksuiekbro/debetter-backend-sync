package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.tournament.announcement.Comment;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.dto.tournament.announcement.in.CommentDto;
import com.heliozz10.debetter.dto.tournament.announcement.out.AnnouncementView;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.announcement.AnnouncementRepository;
import com.heliozz10.debetter.repository.tournament.announcement.CommentRepository;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    private final UserMapper userMapper;

    private final FileService fileService;

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
    public Announcement addAnnouncementToTournament(AnnouncementFormDto announcementFormDto, MultipartFile image, Long tournamentId, Long creatorId) {
        Tournament tournament = tournamentRepository.getReferenceById(tournamentId);

        Announcement announcement = announcementMapper.toAnnouncement(announcementFormDto);

        OrganizerProfile author = organizerProfileRepository.getReferenceById(creatorId);

        announcement.setTournament(tournament);
        announcement.setAuthor(author);
        announcement.setTimestamp(LocalDateTime.now());
        announcement.setHidden(false);

        if(image != null) {
            Url url = fileService.uploadFile(image, "announcements", tournament.getId().toString());
            announcement.setImageUrl(url);
        }

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
    public Announcement updateAnnouncement(AnnouncementFormDto announcementFormDto, MultipartFile image, Long tournamentId, Long announcementId, Long editorId) {
        Announcement announcement = updateLastEdited(announcementId, editorId);

        if(!Objects.equals(announcement.getTournament().getId(), tournamentId)) {
            throw new EntityNotFoundException("Announcement not found");
        }

        if(image != null) {
            if(announcement.getImageUrl() != null) {
                fileService.deleteFile(announcement.getImageUrl());
            }
            Url url = fileService.uploadFile(image, "announcements", tournamentId.toString());
            announcement.setImageUrl(url);
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
        Announcement announcement = announcementRepository.findByTournamentIdAndId(tournamentId, announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        return announcement.getComments();
    }

    @Transactional
    public void addCommentToAnnouncement(Long tournamentId, Long announcementId, Long authorId, CommentDto dto) {
        Announcement announcement = announcementRepository.findByTournamentIdAndId(tournamentId, announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        User author = entityManager.getReference(User.class, authorId);

        Comment comment = new Comment(dto.content(), LocalDateTime.now(), announcement, author);

        announcement.getComments().add(comment);

        commentRepository.save(comment);
    }

    @Transactional
    public void removeCommentFromAnnouncement(Long authorId, Long commentId) {
        Comment comment = commentRepository.findByAuthorIdAndId(authorId, commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        comment.getAnnouncement().getComments().removeIf(c -> Objects.equals(c.getId(), commentId));
    }

    @Transactional
    public void removeAnnouncementFromTournament(Long announcementId, Long tournamentId) {
        Announcement announcement = announcementRepository.findByTournamentIdAndId(tournamentId, announcementId)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found"));

        announcementRepository.deleteById(announcementId);
    }

    public AnnouncementView toAnnouncementView(Announcement announcement) {
        AnnouncementView view = announcementMapper.toAnnouncementView(announcement);
        view.setUser(userMapper.toSimpleUserView(announcement.getAuthor().getUser()));
        return view;
    }
}
