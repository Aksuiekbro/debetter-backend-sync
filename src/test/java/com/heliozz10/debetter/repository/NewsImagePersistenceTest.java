package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.search.enabled=false")
@Import(NewsImagePersistenceTest.CacheTestConfiguration.class)
class NewsImagePersistenceTest {
    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void clearingGalleryPersistsAfterFlushAndReload() {
        News news = saveNewsWithImages("first.jpg", "second.jpg");

        News managedNews = reload(news.getId());
        managedNews.getImages().clear();
        newsRepository.flush();
        entityManager.clear();

        assertTrue(newsRepository.findById(news.getId()).orElseThrow().getImages().isEmpty());
    }

    @Test
    void reorderedGalleryPersistsAfterFlushAndReload() {
        News news = saveNewsWithImages("first.jpg", "second.jpg", "third.jpg");

        News managedNews = reload(news.getId());
        Url firstImage = imageNamed(managedNews, "first.jpg");
        Url thirdImage = imageNamed(managedNews, "third.jpg");
        managedNews.getImages().clear();
        managedNews.getImages().add(thirdImage);
        managedNews.getImages().add(firstImage);
        thirdImage.setNewsImageOrder(0);
        firstImage.setNewsImageOrder(1);
        newsRepository.flush();
        entityManager.clear();

        News reloadedNews = newsRepository.findById(news.getId()).orElseThrow();
        assertEquals(
                List.of("third.jpg", "first.jpg"),
                reloadedNews.getImages().stream().map(Url::getUrl).toList()
        );
    }

    @Test
    void legacyGalleryWithoutPositionsStillLoadsInStableOrder() {
        News news = saveNewsWithImages("first.jpg", "second.jpg", "third.jpg");

        News reloadedNews = reload(news.getId());

        assertEquals(
                List.of("first.jpg", "second.jpg", "third.jpg"),
                reloadedNews.getImages().stream().map(Url::getUrl).toList()
        );
        assertTrue(reloadedNews.getImages().stream().allMatch(image -> image.getNewsImageOrder() == null));
    }

    @Test
    void deletingNewsWithMediaRemovesItsOwnedUrlRows() {
        News news = saveNewsWithImages("first.jpg", "second.jpg");
        news.setThumbnailUrl(image("cover.jpg"));
        news = newsRepository.saveAndFlush(news);
        long newsId = news.getId();
        long thumbnailId = news.getThumbnailUrl().getId();
        List<Long> galleryIds = news.getImages().stream().map(Url::getId).toList();

        // File cleanup must not delete URL rows while the News row still owns them.
        newsRepository.deleteById(newsId);
        newsRepository.flush();
        entityManager.clear();

        assertTrue(newsRepository.findById(newsId).isEmpty());
        assertTrue(urlRepository.findById(thumbnailId).isEmpty());
        assertTrue(galleryIds.stream().noneMatch(urlRepository::existsById));
    }

    private News saveNewsWithImages(String... imageNames) {
        News news = new News();
        news.setTitle("Gallery persistence regression");
        news.setContent("The gallery must preserve explicit organizer ordering.");
        news.setTimestamp(LocalDateTime.now());
        news.setImages(new ArrayList<>(List.of(imageNames).stream().map(this::image).toList()));
        return newsRepository.saveAndFlush(news);
    }

    private News reload(long newsId) {
        entityManager.clear();
        return newsRepository.findById(newsId).orElseThrow();
    }

    private Url image(String name) {
        Url image = new Url();
        image.setUrl(name);
        return image;
    }

    private Url imageNamed(News news, String name) {
        return news.getImages().stream()
                .filter(image -> name.equals(image.getUrl()))
                .findFirst()
                .orElseThrow();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("userTournamentPermissions");
        }
    }
}
