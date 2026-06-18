package com.heliozz10.debetter.service;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.tag.TagType;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.mapper.NewsMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.NewsRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
        NewsDto dto = new NewsDto("Registration is open", "Teams can now register for the tournament.", List.of("Info"));
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
    void deleteNewsTreatsMissingMediaAsAbsent() {
        News news = new News();
        news.setImages(null);
        news.setThumbnailUrl(null);

        when(newsRepository.findByAuthorIdAndId(7L, 11L)).thenReturn(Optional.of(news));

        newsService.deleteNews(11L, 7L);

        verify(fileService).deleteFile(null);
        verify(fileService).deleteFiles(null);
        verify(newsRepository).deleteById(11L);
    }
}
