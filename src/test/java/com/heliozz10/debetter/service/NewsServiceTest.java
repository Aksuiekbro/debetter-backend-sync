package com.heliozz10.debetter.service;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tag.TagType;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.mapper.NewsMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.NewsRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @Mock
    private TagService tagService;

    @Mock
    private FileService fileService;

    @Mock
    private UserMapper userMapper;

    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsService(entityManager, newsRepository, newsMapper, tagService, fileService, userMapper);
    }

    @Test
    void createNewsTreatsMissingGalleryImagesAsEmptyList() {
        NewsDto dto = new NewsDto(
                "Registration is open",
                "Teams can now register for the tournament.",
                List.of("Info"),
                null
        );
        News news = new News();
        OrganizerProfile author = new OrganizerProfile();

        when(newsMapper.toNews(dto)).thenReturn(news);
        when(entityManager.getReference(OrganizerProfile.class, 7L)).thenReturn(author);
        when(fileService.uploadImages(anyMap(), eq("news/images"))).thenReturn(List.of());
        when(tagService.findOrCreateTags(TagType.NEWS, dto.tags())).thenReturn(List.of());
        when(newsRepository.save(news)).thenReturn(news);

        News created = newsService.createNews(dto, null, null, 7L);

        assertSame(news, created);
        assertSame(author, news.getAuthor());
        assertEquals(List.of(), news.getImages());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, MultipartFile>> imagesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fileService).uploadImages(imagesCaptor.capture(), eq("news/images"));
        assertTrue(imagesCaptor.getValue().isEmpty());
    }

    @Test
    void updateNewsWithoutRetainedIdsOrUploadsPreservesExistingImages() {
        Url firstImage = image(101L);
        Url secondImage = image(102L);
        News news = newsWithImages(firstImage, secondImage);
        NewsDto dto = new NewsDto("Updated title", "Updated content", List.of("Update"), null);

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));
        when(tagService.findOrCreateTags(TagType.NEWS, dto.tags())).thenReturn(List.of());
        when(newsRepository.save(news)).thenReturn(news);

        News updated = newsService.updateNews(dto, null, null, 11L, 7L);

        assertSame(news, updated);
        assertEquals(List.of(firstImage, secondImage), news.getImages());
        verify(fileService, never()).uploadImages(anyMap(), anyString());
        verify(fileService, never()).deletePhysicalFilesAfterCommit(any());
    }

    @Test
    void updateNewsKeepsRequestedOrderAppendsUploadsAndDeletesOnlyRemovedImages() {
        Url firstImage = image(101L);
        Url removedImage = image(102L);
        Url thirdImage = image(103L);
        Url firstUploadedImage = image(201L);
        Url secondUploadedImage = image(202L);
        MultipartFile firstUpload = mock(MultipartFile.class);
        MultipartFile secondUpload = mock(MultipartFile.class);
        News news = newsWithImages(firstImage, removedImage, thirdImage);
        List<Url> managedImages = news.getImages();
        NewsDto dto = new NewsDto(
                "Updated title",
                "Updated content",
                null,
                List.of(103L, 101L),
                List.of(2, 3)
        );

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));
        when(fileService.uploadImages(anyMap(), eq("news/images")))
                .thenReturn(List.of(firstUploadedImage, secondUploadedImage));
        when(newsRepository.save(news)).thenReturn(news);

        News updated = newsService.updateNews(
                dto,
                null,
                List.of(firstUpload, secondUpload),
                11L,
                7L
        );

        assertSame(news, updated);
        assertSame(managedImages, news.getImages());
        assertEquals(
                List.of(thirdImage, firstImage, firstUploadedImage, secondUploadedImage),
                news.getImages()
        );
        assertEquals(List.of(0, 1, 2, 3), news.getImages().stream().map(Url::getNewsImageOrder).toList());
        verify(fileService).deletePhysicalFilesAfterCommit(List.of(removedImage));
    }

    @Test
    void updateNewsCanPlaceANewUploadBeforeRetainedImages() {
        Url firstImage = image(101L);
        Url secondImage = image(102L);
        Url uploadedImage = image(201L);
        MultipartFile upload = mock(MultipartFile.class);
        News news = newsWithImages(firstImage, secondImage);
        NewsDto dto = new NewsDto(
                "Updated title",
                "Updated content",
                null,
                List.of(101L, 102L),
                List.of(0)
        );

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));
        when(fileService.uploadImages(anyMap(), eq("news/images"))).thenReturn(List.of(uploadedImage));
        when(newsRepository.save(news)).thenReturn(news);

        newsService.updateNews(dto, null, List.of(upload), 11L, 7L);

        assertEquals(List.of(uploadedImage, firstImage, secondImage), news.getImages());
        assertEquals(List.of(0, 1, 2), news.getImages().stream().map(Url::getNewsImageOrder).toList());
    }

    @ParameterizedTest
    @MethodSource("invalidNewImagePositionRequests")
    void updateNewsRejectsInvalidNewImagePositionsBeforeUploading(
            List<Integer> newImagePositions,
            int uploadCount
    ) {
        Url existingImage = image(101L);
        News news = newsWithImages(existingImage);
        NewsDto dto = new NewsDto(
                "Updated title",
                "Updated content",
                null,
                List.of(101L),
                newImagePositions
        );
        List<MultipartFile> uploads = new ArrayList<>();
        for(int index = 0; index < uploadCount; index++) {
            uploads.add(mock(MultipartFile.class));
        }

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        assertThrows(
                IllegalArgumentException.class,
                () -> newsService.updateNews(dto, null, uploads, 11L, 7L)
        );

        assertEquals(List.of(existingImage), news.getImages());
        verify(fileService, never()).uploadImages(anyMap(), anyString());
        verify(newsMapper, never()).updateNews(any(), any());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void updateNewsValidatesRetainedImagesBeforeReplacingThumbnail() {
        Url existingThumbnail = image(100L);
        Url existingImage = image(101L);
        MultipartFile replacementThumbnail = mock(MultipartFile.class);
        News news = newsWithImages(existingImage);
        news.setThumbnailUrl(existingThumbnail);
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, List.of(999L));

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        assertThrows(
                IllegalArgumentException.class,
                () -> newsService.updateNews(dto, replacementThumbnail, null, 11L, 7L)
        );

        assertAll(
                () -> assertSame(existingThumbnail, news.getThumbnailUrl()),
                () -> verify(fileService, never()).uploadImage(any(), anyString(), anyString()),
                () -> verify(fileService, never()).deletePhysicalFileAfterCommit(any()),
                () -> verify(newsRepository, never()).save(any())
        );
    }

    @Test
    void updateNewsRejectsRetainedImageFromAnotherPostBeforeUploading() {
        Url existingImage = image(101L);
        MultipartFile upload = mock(MultipartFile.class);
        News news = newsWithImages(existingImage);
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, List.of(999L));

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        assertThrows(
                IllegalArgumentException.class,
                () -> newsService.updateNews(dto, null, List.of(upload), 11L, 7L)
        );

        assertEquals(List.of(existingImage), news.getImages());
        verify(fileService, never()).uploadImages(anyMap(), anyString());
        verify(fileService, never()).deletePhysicalFilesAfterCommit(any());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void updateNewsRejectsDuplicateRetainedImageIdsBeforeUploading() {
        Url existingImage = image(101L);
        MultipartFile upload = mock(MultipartFile.class);
        News news = newsWithImages(existingImage);
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, List.of(101L, 101L));

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        assertThrows(
                IllegalArgumentException.class,
                () -> newsService.updateNews(dto, null, List.of(upload), 11L, 7L)
        );

        assertEquals(List.of(existingImage), news.getImages());
        verify(fileService, never()).uploadImages(anyMap(), anyString());
        verify(fileService, never()).deletePhysicalFilesAfterCommit(any());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void updateNewsRejectsMoreThanTenRetainedAndUploadedImagesBeforeUploading() {
        List<Url> existingImages = LongStream.rangeClosed(1L, 10L)
                .mapToObj(NewsServiceTest::image)
                .toList();
        List<Long> retainedImageIds = existingImages.stream().map(Url::getId).toList();
        MultipartFile eleventhImage = mock(MultipartFile.class);
        News news = new News();
        news.setImages(new ArrayList<>(existingImages));
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, retainedImageIds);

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        assertThrows(
                IllegalArgumentException.class,
                () -> newsService.updateNews(dto, null, List.of(eleventhImage), 11L, 7L)
        );

        assertEquals(existingImages, news.getImages());
        verify(fileService, never()).uploadImages(anyMap(), anyString());
        verify(fileService, never()).deletePhysicalFilesAfterCommit(any());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void updateNewsWithNullTagsPreservesExistingTags() {
        Tag existingTag = new Tag();
        News news = newsWithImages();
        news.setTags(new ArrayList<>(List.of(existingTag)));
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, null);

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenReturn(news);

        newsService.updateNews(dto, null, null, 11L, 7L);

        assertEquals(List.of(existingTag), news.getTags());
        verify(tagService, never()).findOrCreateTags(any(), any());
    }

    @Test
    void updateNewsUploadsImagesInRequestOrder() {
        MultipartFile firstUpload = mock(MultipartFile.class);
        MultipartFile secondUpload = mock(MultipartFile.class);
        MultipartFile thirdUpload = mock(MultipartFile.class);
        News news = newsWithImages();
        NewsDto dto = new NewsDto("Updated title", "Updated content", null, List.of());

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));
        when(fileService.uploadImages(anyMap(), eq("news/images"))).thenReturn(List.of());
        when(newsRepository.save(news)).thenReturn(news);

        newsService.updateNews(
                dto,
                null,
                List.of(firstUpload, secondUpload, thirdUpload),
                11L,
                7L
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, MultipartFile>> imagesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fileService).uploadImages(imagesCaptor.capture(), eq("news/images"));
        Map<String, MultipartFile> uploadedImages = imagesCaptor.getValue();
        assertInstanceOf(LinkedHashMap.class, uploadedImages);
        assertEquals(List.of(firstUpload, secondUpload, thirdUpload), new ArrayList<>(uploadedImages.values()));
    }

    @Test
    void deleteNewsTreatsMissingMediaAsAbsent() {
        News news = new News();
        news.setImages(null);
        news.setThumbnailUrl(null);

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        newsService.deleteNews(11L, 7L);

        verify(fileService).deletePhysicalFileAfterCommit(null);
        verify(fileService).deletePhysicalFilesAfterCommit(null);
        verify(newsRepository).deleteById(11L);
    }

    private static News newsWithImages(Url... images) {
        News news = new News();
        news.setImages(new ArrayList<>(List.of(images)));
        return news;
    }

    private static Stream<Arguments> invalidNewImagePositionRequests() {
        return Stream.of(
                Arguments.of(List.of(), 1),
                Arguments.of(List.of(0, 0), 2),
                Arguments.of(List.of(2), 1)
        );
    }

    private static Url image(long id) {
        Url image = new Url();
        image.setId(id);
        image.setUrl("/uploads/images/news/images/" + id + ".jpg");
        return image;
    }
}
