package com.heliozz10.debetter.mapper.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.util.media.out.UrlView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UrlMapper {
    UrlView toUrlView(Url url);

    List<UrlView> toUrlViews(List<Url> urls);
}
