package com.heliozz10.debetter.mapper.tournament.round;

import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import com.heliozz10.debetter.dto.tournament.round.in.RoundUpdateDto;
import com.heliozz10.debetter.dto.tournament.round.out.RoundView;
import com.heliozz10.debetter.dto.tournament.round.out.SimpleRoundView;
import com.heliozz10.debetter.mapper.tournament.MatchMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class RoundMapper {
    @Autowired
    protected MatchMapper matchMapper;

    public abstract void updateRound(RoundUpdateDto dto, @MappingTarget Round round);

    public abstract SimpleRoundView toSimpleRoundView(Round round);

    public abstract List<SimpleRoundView> toSimpleRoundViews(List<Round> rounds);

    @InheritConfiguration(name = "toSimpleRoundView")
    @Mapping(target = "matches", ignore = true)
    public abstract RoundView toRoundView(Round round);

    @AfterMapping
    protected void mapMatchesIfPublic(Round round, @MappingTarget RoundView roundView) {
        if (Boolean.TRUE.equals(round.getMatchesArePublic())) {
            roundView.setMatches(matchMapper.toMatchViews(round.getMatches()));
        }
    }

    public abstract List<RoundView> toRoundViews(List<Round> rounds);
}
