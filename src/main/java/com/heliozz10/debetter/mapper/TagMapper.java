package com.heliozz10.debetter.mapper;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.dto.tag.out.TagView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagView toTagView(Tag tag);

    List<TagView> toTagViews(List<Tag> tags);
}
