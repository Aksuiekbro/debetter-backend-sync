package com.heliozz10.debetter.dto.tournament.announcement.out;

import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentView {
    private Long id;
    private String content;
    private LocalDateTime timestamp;
    private SimpleUserView author;
}
