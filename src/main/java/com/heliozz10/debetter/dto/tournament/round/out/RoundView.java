package com.heliozz10.debetter.dto.tournament.round.out;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoundView extends SimpleRoundView {
    private List<MatchView> matches;
}
