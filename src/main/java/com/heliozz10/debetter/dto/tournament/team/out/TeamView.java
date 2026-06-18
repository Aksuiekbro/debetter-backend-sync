package com.heliozz10.debetter.dto.tournament.team.out;

import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamView extends SimpleTeamView {
    private Integer preliminaryScore;
    private Boolean active;
    private Boolean checkedIn;
    private Boolean disqualified;
    private List<SimpleTournamentParticipantView> members;
}
