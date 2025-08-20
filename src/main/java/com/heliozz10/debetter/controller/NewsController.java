package com.heliozz10.debetter.controller;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.in.NewsGetParams;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.mapper.NewsMapper;
import com.heliozz10.debetter.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/news")
public class NewsController {
    private final NewsService newsService;
    private final NewsMapper newsMapper;

    @GetMapping
    public PageableResult<NewsView> getNews(
            @ModelAttribute NewsGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<News> news = newsService.getNews(params, pageable);
        return new PageableResult<>(
                newsMapper.toNewsViews(news.getContent()),
                news.getTotalElements(),
                news.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public NewsView getNewsById(@PathVariable Long id) {
        return newsMapper.toNewsView(newsService.getNewsById(id));
    }

    @PostMapping
    public NewsView createNews(@RequestBody NewsDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return newsMapper.toNewsView(newsService.createNews(dto, profile.getId()));
    }

    @PatchMapping("/{id}")
    public NewsView updateNews(@PathVariable Long id, @RequestBody NewsDto dto) {
        return newsMapper.toNewsView(newsService.updateNews(dto, id));
    }

    @DeleteMapping("/{id}")
    public void deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
    }
}
