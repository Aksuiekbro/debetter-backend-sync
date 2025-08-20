package com.heliozz10.debetter.mapper;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {
                TagMapper.class,
                OrganizerProfileMapper.class
        },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NewsMapper {
    News toNews(NewsDto newsDto);
    void updateNews(NewsDto newsDto, @MappingTarget News news);

    NewsView toNewsView(News news);
    List<NewsView> toNewsViews(List<News> news);
}
