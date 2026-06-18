package com.heliozz10.debetter.dto.tournament.round.out;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import lombok.Data;

import java.util.List;

@Data
public class RoundGroupView {
    private Long id;
    private RoundGroupType type;
    private DebateFormat format;
    private List<SimpleRoundView> rounds;
    private Integer currentRoundNumber;
}
