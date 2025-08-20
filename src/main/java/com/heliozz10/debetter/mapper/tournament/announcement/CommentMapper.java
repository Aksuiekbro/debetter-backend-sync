package com.heliozz10.debetter.mapper.tournament.announcement;

import com.heliozz10.debetter.content.tournament.announcement.Comment;
import com.heliozz10.debetter.dto.tournament.announcement.out.CommentView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        CommentMapper.class
})
public interface CommentMapper {
    CommentView toCommentView(Comment comment);

    List<CommentView> toCommentViews(List<Comment> announcementComments);
}
