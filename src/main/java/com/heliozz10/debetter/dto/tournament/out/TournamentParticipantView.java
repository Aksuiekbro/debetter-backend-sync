package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TournamentParticipantView extends SimpleTournamentParticipantView {
    private SimpleTeamView team;
}
