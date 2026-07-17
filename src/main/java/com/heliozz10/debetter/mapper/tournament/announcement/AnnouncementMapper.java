package com.heliozz10.debetter.mapper.tournament.announcement;

import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.dto.tournament.announcement.in.AnnouncementFormDto;
import com.heliozz10.debetter.dto.tournament.announcement.out.AnnouncementView;
import com.heliozz10.debetter.mapper.TagMapper;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
                OrganizerProfileMapper.class,
                CommentMapper.class,
                TagMapper.class
        }
)
public interface AnnouncementMapper {
    @Mapping(target = "tags", ignore = true)
    Announcement toAnnouncement(AnnouncementFormDto dto);

    @Mapping(target = "tags", ignore = true)
    void updateAnnouncement(AnnouncementFormDto dto, @MappingTarget Announcement announcement);

    AnnouncementView toAnnouncementView(Announcement announcement);

    List<AnnouncementView> toAnnouncementViews(List<Announcement> announcements);
}
