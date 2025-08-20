package com.heliozz10.debetter.dto.tournament.round.out;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import lombok.Data;

import java.util.List;

@Data
public class RoundView {
    private Long id;
    private String name;
    private DebateFormat customFormat;
    private Integer roundNumber;
    private Boolean matchesArePublic;
    private List<MatchView> matches;
}
