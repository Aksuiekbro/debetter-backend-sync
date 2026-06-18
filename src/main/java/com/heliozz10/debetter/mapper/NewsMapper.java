package com.heliozz10.debetter.mapper;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import com.heliozz10.debetter.mapper.util.media.UrlMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                TagMapper.class,
                OrganizerProfileMapper.class,
                UrlMapper.class
        },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NewsMapper {
    @Mapping(target = "tags", ignore = true)
    News toNews(NewsDto newsDto);

    @Mapping(target = "tags", ignore = true)
    void updateNews(NewsDto newsDto, @MappingTarget News news);

    NewsView toNewsView(News news);
    List<NewsView> toNewsViews(List<News> news);
}
