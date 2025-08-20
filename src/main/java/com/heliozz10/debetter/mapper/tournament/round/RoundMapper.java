package com.heliozz10.debetter.mapper.tournament.round;

import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.dto.tournament.round.out.RoundView;
import com.heliozz10.debetter.mapper.tournament.MatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        MatchMapper.class
})
public interface RoundMapper {
    @Mapping(target = "matches", expression = "java(round.getMatchesArePublic() ? matchMapper.toMatchViewList(round.getMatches()) : null)")
    RoundView toRoundView(Round round);

    List<RoundView> toRoundViews(List<Round> rounds);
}
