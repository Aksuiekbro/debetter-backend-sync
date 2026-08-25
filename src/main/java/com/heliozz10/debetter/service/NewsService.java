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
    private static final int MAX_GALLERY_IMAGES = 10;

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
        setCreateFields(newsDto, thumbnail, images, news);
        news.setTimestamp(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Transactional
    public News updateNews(NewsDto newsDto, MultipartFile thumbnail, List<MultipartFile> images, Long newsId, Long authorId) {
        News news = newsRepository.findByAuthorIdAndId(authorId, newsId)
                .orElseThrow(() -> new EntityNotFoundException("News not found"));
        GalleryUpdatePlan galleryUpdate = planGalleryUpdate(
                newsDto.retainedImageIds(),
                newsDto.newImagePositions(),
                images,
                news
        );

        newsMapper.updateNews(newsDto, news);
        updateThumbnail(thumbnail, news);
        applyGalleryUpdate(galleryUpdate, news);
        if(newsDto.tags() != null) {
            news.setTags(tagService.findOrCreateTags(TagType.NEWS, newsDto.tags()));
        }

        news.setLastEdited(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(Long newsId, Long authorId) {
        News news = newsRepository.findByAuthorIdAndId(authorId, newsId)
                .orElseThrow(() -> new EntityNotFoundException("News not found"));
        fileService.deletePhysicalFileAfterCommit(news.getThumbnailUrl());
        fileService.deletePhysicalFilesAfterCommit(news.getImages());
        newsRepository.deleteById(newsId);
    }

    private void setCreateFields(NewsDto newsDto, MultipartFile thumbnail, List<MultipartFile> images, News news) {
        List<MultipartFile> galleryImages = safeImages(images);
        validateImageCount(galleryImages.size());
        updateThumbnail(thumbnail, news);
        List<Url> uploadedImages = uploadGalleryImages(galleryImages);
        assignNewsImageOrder(uploadedImages);
        news.setImages(uploadedImages);
        news.setTags(tagService.findOrCreateTags(TagType.NEWS, newsDto.tags()));
    }

    private void updateThumbnail(MultipartFile thumbnail, News news) {
        if(thumbnail == null) {
            return;
        }

        Url previousThumbnail = news.getThumbnailUrl();
        Url thumbnailUrl = fileService.uploadImage(
                thumbnail,
                "news/thumbnails",
                UUID.randomUUID().toString()
        );
        news.setThumbnailUrl(thumbnailUrl);

        if(previousThumbnail != null) {
            fileService.deletePhysicalFileAfterCommit(previousThumbnail);
        }
    }

    private GalleryUpdatePlan planGalleryUpdate(
            List<Long> retainedImageIds,
            List<Integer> newImagePositions,
            List<MultipartFile> images,
            News news
    ) {
        List<Url> currentImages = news.getImages() == null ? List.of() : List.copyOf(news.getImages());
        List<MultipartFile> galleryImages = safeImages(images);

        if(retainedImageIds == null && galleryImages.isEmpty()
                && (newImagePositions == null || newImagePositions.isEmpty())) {
            return GalleryUpdatePlan.unchanged();
        }

        List<Long> requestedIds = retainedImageIds == null
                ? currentImages.stream().map(Url::getId).toList()
                : retainedImageIds;

        if(new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new IllegalArgumentException("Retained image IDs must be unique");
        }

        Map<Long, Url> currentImagesById = currentImages.stream().collect(
                LinkedHashMap::new,
                (map, image) -> map.put(image.getId(), image),
                LinkedHashMap::putAll
        );
        List<Url> retainedImages = new ArrayList<>(requestedIds.size());
        for(Long imageId : requestedIds) {
            Url retainedImage = currentImagesById.get(imageId);
            if(retainedImage == null) {
                throw new IllegalArgumentException("Cannot retain an image that does not belong to this News post");
            }
            retainedImages.add(retainedImage);
        }

        validateImageCount(retainedImages.size() + galleryImages.size());
        List<Integer> requestedNewImagePositions = validateNewImagePositions(
                newImagePositions,
                retainedImages.size(),
                galleryImages.size()
        );

        Set<Long> retainedIds = new HashSet<>(requestedIds);
        List<Url> removedImages = currentImages.stream()
                .filter(image -> !retainedIds.contains(image.getId()))
                .toList();

        return new GalleryUpdatePlan(
                true,
                retainedImages,
                galleryImages,
                requestedNewImagePositions,
                removedImages
        );
    }

    private void applyGalleryUpdate(GalleryUpdatePlan update, News news) {
        if(!update.requested()) {
            return;
        }

        List<Url> uploadedImages = update.newImages().isEmpty()
                ? List.of()
                : uploadGalleryImages(update.newImages());

        List<Url> updatedImages = mergeGalleryImages(
                update.retainedImages(),
                uploadedImages,
                update.newImagePositions()
        );
        assignNewsImageOrder(updatedImages);

        List<Url> managedImages = news.getImages();
        if(managedImages == null) {
            news.setImages(new ArrayList<>(updatedImages));
        } else {
            managedImages.clear();
            managedImages.addAll(updatedImages);
        }

        if(!update.removedImages().isEmpty()) {
            fileService.deletePhysicalFilesAfterCommit(update.removedImages());
        }
    }

    private List<Integer> validateNewImagePositions(
            List<Integer> requestedPositions,
            int retainedImageCount,
            int newImageCount
    ) {
        if(requestedPositions == null) {
            return java.util.stream.IntStream.range(retainedImageCount, retainedImageCount + newImageCount)
                    .boxed()
                    .toList();
        }
        if(requestedPositions.size() != newImageCount) {
            throw new IllegalArgumentException("Each new gallery image must have exactly one position");
        }

        int totalImageCount = retainedImageCount + newImageCount;
        Set<Integer> uniquePositions = new HashSet<>();
        for(Integer position : requestedPositions) {
            if(position == null || position < 0 || position >= totalImageCount) {
                throw new IllegalArgumentException("New gallery image position is outside the gallery");
            }
            if(!uniquePositions.add(position)) {
                throw new IllegalArgumentException("New gallery image positions must be unique");
            }
        }
        return List.copyOf(requestedPositions);
    }

    private List<Url> mergeGalleryImages(
            List<Url> retainedImages,
            List<Url> uploadedImages,
            List<Integer> newImagePositions
    ) {
        Map<Integer, Url> uploadedImagesByPosition = new HashMap<>();
        for(int index = 0; index < uploadedImages.size(); index++) {
            uploadedImagesByPosition.put(newImagePositions.get(index), uploadedImages.get(index));
        }

        int totalImageCount = retainedImages.size() + uploadedImages.size();
        List<Url> orderedImages = new ArrayList<>(totalImageCount);
        Iterator<Url> retainedImagesIterator = retainedImages.iterator();
        for(int position = 0; position < totalImageCount; position++) {
            Url uploadedImage = uploadedImagesByPosition.get(position);
            orderedImages.add(uploadedImage != null ? uploadedImage : retainedImagesIterator.next());
        }
        return orderedImages;
    }

    private void assignNewsImageOrder(List<Url> images) {
        for(int index = 0; index < images.size(); index++) {
            images.get(index).setNewsImageOrder(index);
        }
    }

    private List<Url> uploadGalleryImages(List<MultipartFile> images) {
        Map<String, MultipartFile> imagesMap = new LinkedHashMap<>();
        for(MultipartFile image : images) {
            imagesMap.put(UUID.randomUUID().toString(), image);
        }
        return fileService.uploadImages(imagesMap, "news/images");
    }

    private List<MultipartFile> safeImages(List<MultipartFile> images) {
        return images == null ? List.of() : images;
    }

    private void validateImageCount(int imageCount) {
        if(imageCount > MAX_GALLERY_IMAGES) {
            throw new IllegalArgumentException("A News post can contain at most 10 gallery images");
        }
    }

    private record GalleryUpdatePlan(
            boolean requested,
            List<Url> retainedImages,
            List<MultipartFile> newImages,
            List<Integer> newImagePositions,
            List<Url> removedImages
    ) {
        private static GalleryUpdatePlan unchanged() {
            return new GalleryUpdatePlan(false, List.of(), List.of(), List.of(), List.of());
        }
    }

    public NewsView toNewsView(News news) {
        NewsView view = newsMapper.toNewsView(news);
        view.setUser(userMapper.toSimpleUserView(news.getAuthor().getUser()));
        return view;
    }
}
