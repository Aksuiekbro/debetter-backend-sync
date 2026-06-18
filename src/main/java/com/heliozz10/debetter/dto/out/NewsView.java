package com.heliozz10.debetter.dto.out;

import com.heliozz10.debetter.dto.tag.out.TagView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NewsView {
    private Long id;
    private OrganizerProfileView author;
    private SimpleUserView user;
    private UrlView thumbnailUrl;
    private List<UrlView> images;
    private String title;
    private String content;
    private List<TagView> tags;
    private LocalDateTime timestamp;
}
