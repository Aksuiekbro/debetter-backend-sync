package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Feedback;
import com.heliozz10.debetter.dto.tournament.in.FeedbackDto;
import com.heliozz10.debetter.dto.tournament.out.FeedbackView;
import com.heliozz10.debetter.mapper.TagMapper;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        ParticipantProfileMapper.class,
        TagMapper.class
})
public interface FeedbackMapper {
    Feedback toFeedback(FeedbackDto dto);
    void updateFeedback(FeedbackDto dto, @MappingTarget Feedback feedback);

    FeedbackView toFeedbackView(Feedback feedback);

    List<FeedbackView> toFeedbackViews(List<Feedback> feedbacks);
}
