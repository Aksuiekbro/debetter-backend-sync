package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.dto.tag.out.TagView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.profile.out.ParticipantProfileView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedbackView {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Boolean edited;
    private ParticipantProfileView author;
    private SimpleUserView user;
    private List<TagView> tags;
}
