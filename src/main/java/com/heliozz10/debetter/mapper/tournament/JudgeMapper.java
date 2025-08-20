package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.util.socials.SocialProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        SocialProfileMapper.class
})
public interface JudgeMapper {
    Judge toJudge(JudgeFormDto dto);
    void updateJudge(JudgeFormDto dto, @MappingTarget Judge judge);

    JudgeView toJudgeView(Judge judge);

    List<JudgeView> toJudgeViews(List<Judge> judges);
}
