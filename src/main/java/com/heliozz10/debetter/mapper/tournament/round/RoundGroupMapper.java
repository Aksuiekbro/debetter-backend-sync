package com.heliozz10.debetter.mapper.tournament.round;

import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.dto.tournament.round.out.RoundGroupView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        RoundMapper.class
})
public interface RoundGroupMapper {
    RoundGroupView toRoundGroupView(RoundGroup roundGroup);

    List<RoundGroupView> toRoundGroupViews(List<RoundGroup> roundGroups);
}
