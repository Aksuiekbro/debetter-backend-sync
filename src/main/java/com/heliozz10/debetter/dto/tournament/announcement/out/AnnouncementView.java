package com.heliozz10.debetter.dto.tournament.announcement.out;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tag.out.TagView;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementView {
    private Long id;
    private String title;
    private String content;
    private Url imageUrl;
    private LocalDateTime timestamp;
    private OrganizerProfileView author;
    private List<CommentView> comments;
    private List<TagView> tags;
}
