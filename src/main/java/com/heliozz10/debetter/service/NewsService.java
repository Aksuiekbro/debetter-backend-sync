package com.heliozz10.debetter.service;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.tag.TagType;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.in.NewsGetParams;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.mapper.NewsMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
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

    private final UserMapper userMapper;

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
    public News createNews(NewsDto newsDto, MultipartFile thumbnail, List<MultipartFile> images, Long authorId) {
        News news = newsMapper.toNews(newsDto);

        news.setAuthor(entityManager.getReference(OrganizerProfile.class, authorId));

        setUnmappableFields(newsDto, thumbnail, images, news);

        news.setTimestamp(LocalDateTime.now());

        return newsRepository.save(news);
    }

    @Transactional
    public News updateNews(NewsDto newsDto, MultipartFile thumbnail, List<MultipartFile> images, Long newsId, Long authorId) {
        News news = newsRepository.findByAuthorIdAndId(authorId, newsId).orElseThrow(() -> new EntityNotFoundException("News not found"));

        newsMapper.updateNews(newsDto, news);

        setUnmappableFields(newsDto, thumbnail, images, news);

        news.setLastEdited(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(Long newsId, Long authorId) {
        News news = newsRepository.findByAuthorIdAndId(authorId, newsId).orElseThrow(() -> new EntityNotFoundException("News not found"));
        fileService.deleteFile(news.getThumbnailUrl());
        fileService.deleteFiles(news.getImages());
        newsRepository.deleteById(newsId);
    }

    private void setUnmappableFields(NewsDto newsDto, MultipartFile thumbnail, List<MultipartFile> images, News news) {
        final String newsPath = "news/";

        if(thumbnail != null) {
            Url thumbnailUrl = fileService.uploadImage(thumbnail, newsPath + "thumbnails", UUID.randomUUID().toString());
            news.setThumbnailUrl(thumbnailUrl);
        }

        List<MultipartFile> galleryImages = images == null ? List.of() : images;
        Map<String, MultipartFile> imagesMap = new HashMap<>();

        for (MultipartFile image : galleryImages) {
            imagesMap.put(UUID.randomUUID().toString(), image);
        }

        List<Url> imageUrls = fileService.uploadImages(imagesMap, newsPath + "images");

        news.setImages(imageUrls);

        news.setTags(tagService.findOrCreateTags(TagType.NEWS, newsDto.tags()));
    }

    public NewsView toNewsView(News news) {
        NewsView view = newsMapper.toNewsView(news);
        view.setUser(userMapper.toSimpleUserView(news.getAuthor().getUser()));
        return view;
    }
}
