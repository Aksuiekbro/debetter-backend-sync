package com.heliozz10.debetter.controller;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.in.NewsGetParams;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/news")
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public PageableResult<NewsView> getNews(
            @Valid @ModelAttribute NewsGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<News> news = newsService.getNews(params, pageable);
        return new PageableResult<>(
                news.getContent().stream().map(newsService::toNewsView).toList(),
                news.getTotalElements(),
                news.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public NewsView getNewsById(@PathVariable Long id) {
        return newsService.toNewsView(newsService.getNewsById(id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER'")
    @PostMapping
    public NewsView createNews(@Valid @RequestPart("data") NewsDto dto, @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail, @RequestPart(value = "images", required = false) List<MultipartFile> images, Authentication authentication) {
        List<MultipartFile> galleryImages = images == null ? List.of() : images;
        if(galleryImages.size() > 10) {
            throw new IllegalArgumentException("Too many images");
        }
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return newsService.toNewsView(newsService.createNews(dto, thumbnail, galleryImages, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER'")
    @PatchMapping("/{id}")
    public NewsView updateNews(@PathVariable Long id, @Valid @RequestPart("data") NewsDto dto, @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail, @RequestPart(value = "images", required = false) List<MultipartFile> images, Authentication authentication) {
        Long authorId = ((User) authentication.getPrincipal()).getProfile().getId();
        return newsService.toNewsView(newsService.updateNews(dto, thumbnail, images, id, authorId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER'")
    @DeleteMapping("/{id}")
    public void deleteNews(@PathVariable Long id, Authentication authentication) {
        Long authorId = ((User) authentication.getPrincipal()).getProfile().getId();
        newsService.deleteNews(id, authorId);
    }
}
