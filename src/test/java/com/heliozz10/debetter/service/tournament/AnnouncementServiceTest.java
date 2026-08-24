package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.announcement.AnnouncementRepository;
import com.heliozz10.debetter.repository.tournament.announcement.CommentRepository;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.service.TagService;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {
    @Mock
    private EntityManager entityManager;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementMapper announcementMapper;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private OrganizerProfileRepository organizerProfileRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TagService tagService;

    private FileService fileService;
    private AnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class, invocation -> {
            String methodName = invocation.getMethod().getName();
            if (methodName.equals("uploadImage") || methodName.equals("uploadFile")) {
                String path = invocation.getArgument(1);
                String fileName = invocation.getArgument(2);
                Url url = new Url();
                url.setUrl("/uploads/" + path + "/" + fileName + ".jpg");
                return url;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        announcementService = new AnnouncementService(
                entityManager,
                announcementRepository,
                announcementMapper,
                tournamentRepository,
                organizerProfileRepository,
                commentRepository,
                userMapper,
                fileService,
                tagService
        );
    }

    @Test
    void announcementsWithTheSameImageExtensionReceiveDistinctStorageUrls() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        OrganizerProfile author = new OrganizerProfile();
        Announcement firstAnnouncement = new Announcement();
        Announcement secondAnnouncement = new Announcement();
        AnnouncementFormDto dto = new AnnouncementFormDto("Update", "Tournament update", null);
        MockMultipartFile firstImage = new MockMultipartFile(
                "image", "first.jpg", "image/jpeg", "first image".getBytes()
        );
        MockMultipartFile secondImage = new MockMultipartFile(
                "image", "second.jpg", "image/jpeg", "second image".getBytes()
        );

        when(tournamentRepository.getReferenceById(53L)).thenReturn(tournament);
        when(organizerProfileRepository.getReferenceById(7L)).thenReturn(author);
        when(announcementMapper.toAnnouncement(dto)).thenReturn(firstAnnouncement, secondAnnouncement);
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement first = announcementService.addAnnouncementToTournament(dto, firstImage, 53L, 7L);
        Announcement second = announcementService.addAnnouncementToTournament(dto, secondImage, 53L, 7L);

        assertAll(
                () -> assertNotNull(first.getImageUrl()),
                () -> assertNotNull(second.getImageUrl()),
                () -> assertNotEquals(first.getImageUrl().getUrl(), second.getImageUrl().getUrl())
        );
    }

    @Test
    void replacingOneAnnouncementImageUsesANewUrlWithoutTouchingAnotherAnnouncement() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        OrganizerProfile editor = new OrganizerProfile();
        Url originalImage = imageUrl("/uploads/announcements/53.jpg");
        Url otherAnnouncementImage = imageUrl("/uploads/announcements/53-other.jpg");
        Announcement announcement = new Announcement();
        announcement.setId(11L);
        announcement.setTournament(tournament);
        announcement.setImageUrl(originalImage);
        Announcement otherAnnouncement = new Announcement();
        otherAnnouncement.setId(12L);
        otherAnnouncement.setTournament(tournament);
        otherAnnouncement.setImageUrl(otherAnnouncementImage);
        AnnouncementFormDto dto = new AnnouncementFormDto("Updated", "Updated details", null);
        MockMultipartFile replacement = new MockMultipartFile(
                "image", "replacement.jpg", "image/jpeg", "replacement image".getBytes()
        );

        when(announcementRepository.findById(11L)).thenReturn(Optional.of(announcement));
        when(entityManager.getReference(OrganizerProfile.class, 7L)).thenReturn(editor);
        when(announcementRepository.save(announcement)).thenReturn(announcement);
        doAnswer(invocation -> {
            assertSame(originalImage, announcement.getImageUrl());
            return null;
        }).when(fileService).deletePhysicalFileAfterCommit(originalImage);

        Announcement updated = announcementService.updateAnnouncement(dto, replacement, 53L, 11L, 7L);

        assertAll(
                () -> assertSame(announcement, updated),
                () -> assertNotEquals(originalImage.getUrl(), updated.getImageUrl().getUrl()),
                () -> assertSame(otherAnnouncementImage, otherAnnouncement.getImageUrl())
        );
        verify(fileService).deletePhysicalFileAfterCommit(originalImage);
        verify(fileService, never()).deleteFile(originalImage);
        verify(fileService, never()).deleteFile(eq(otherAnnouncementImage));
    }

    @Test
    void removingAnAnnouncementUsesTournamentThenAnnouncementIds() {
        Announcement announcement = new Announcement();
        announcement.setId(11L);
        announcement.setImageUrl(imageUrl("/uploads/announcements/53-unique.jpg"));
        when(announcementRepository.findByTournamentIdAndId(53L, 11L))
                .thenReturn(Optional.of(announcement));

        announcementService.removeAnnouncementFromTournament(53L, 11L);

        verify(fileService).deletePhysicalFileAfterCommit(announcement.getImageUrl());
        verify(announcementRepository).delete(announcement);
    }

    private static Url imageUrl(String value) {
        Url url = new Url();
        url.setUrl(value);
        return url;
    }
}
