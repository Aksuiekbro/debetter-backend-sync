package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.util.socials.SocialProfileMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        SocialProfileMapper.class
})
public interface JudgeMapper {
    Judge toJudge(JudgeFormDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateJudge(JudgeFormDto dto, @MappingTarget Judge judge);

    @Named("organizerJudgeView")
    JudgeView toOrganizerJudgeView(Judge judge);

    @IterableMapping(qualifiedByName = "organizerJudgeView")
    List<JudgeView> toOrganizerJudgeViews(List<Judge> judges);

    @Named("publicJudgeView")
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "email", ignore = true)
    JudgeView toPublicJudgeView(Judge judge);

    @IterableMapping(qualifiedByName = "publicJudgeView")
    List<JudgeView> toPublicJudgeViews(List<Judge> judges);

    default JudgeView toJudgeView(Judge judge) {
        return toOrganizerJudgeView(judge);
    }

    default List<JudgeView> toJudgeViews(List<Judge> judges) {
        return toOrganizerJudgeViews(judges);
    }
}
