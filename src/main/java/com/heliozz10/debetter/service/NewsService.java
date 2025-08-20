package com.heliozz10.debetter.service;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.tag.TagType;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.in.NewsGetParams;
import com.heliozz10.debetter.mapper.NewsMapper;
import com.heliozz10.debetter.repository.NewsRepository;
import com.heliozz10.debetter.repository.specification.NewsSpecification;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class NewsService {
    private final EntityManager entityManager;

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    private final TagService tagService;

    private final FileService fileService;

    @Transactional(readOnly = true)
    public Page<News> getNews(NewsGetParams params, Pageable pageable) {
        Specification<News> specification = NewsSpecification.filterBy(params, entityManager);
        return newsRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public News getNewsById(Long newsId) {
        return newsRepository.findById(newsId).orElseThrow(() -> new EntityNotFoundException("News not found"));
    }

    @Transactional
    public News createNews(NewsDto newsDto, Long authorId) {
        News news = newsMapper.toNews(newsDto);

        news.setAuthor(entityManager.getReference(OrganizerProfile.class, authorId));

        setUnmappableFields(newsDto, news);

        news.setTimestamp(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Transactional
    public News updateNews(NewsDto newsDto, Long newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new EntityNotFoundException("News not found"));

        newsMapper.updateNews(newsDto, news);

        setUnmappableFields(newsDto, news);

        news.setLastEdited(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(Long newsId) {
        newsRepository.findThumbnailUrlByNewsId(newsId).ifPresent(fileService::deleteFile);
        newsRepository.findImagesByNewsId(newsId).forEach(fileService::deleteFile);
        newsRepository.deleteById(newsId);
    }

    private void setUnmappableFields(NewsDto newsDto, News news) {
        final String newsPath = "news/";

        if(newsDto.thumbnail() != null) {
            Url thumbnailUrl = fileService.uploadImage(newsDto.thumbnail(), newsPath + "thumbnails", UUID.randomUUID().toString());
            news.setThumbnailUrl(thumbnailUrl);
        }

        Map<String, MultipartFile> images = new HashMap<>();

        for (int i = 0; i < newsDto.images().size(); i++) {
            images.put(UUID.randomUUID().toString(), newsDto.images().get(i));
        }

        List<Url> imageUrls = fileService.uploadImages(images, newsPath + "images");

        news.setImages(imageUrls);

        news.setTags(tagService.findOrCreateTags(TagType.NEWS, newsDto.tags()));
    }
}
