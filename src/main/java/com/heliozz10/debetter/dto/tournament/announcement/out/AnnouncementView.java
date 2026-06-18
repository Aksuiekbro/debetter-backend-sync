package com.heliozz10.debetter.dto.tournament.announcement.out;

import com.heliozz10.debetter.dto.tag.out.TagView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementView {
    private Long id;
    private String title;
    private String content;
    private UrlView imageUrl;
    private LocalDateTime timestamp;
    private OrganizerProfileView author;
    private SimpleUserView user;
    private List<CommentView> comments;
    private List<TagView> tags;
}
