package com.heliozz10.debetter.dto.tournament.team.out;

import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import lombok.Data;

import java.util.List;

@Data
public class SimpleTeamView {
    private Long id;
    private String name;
    private Club club;
    private List<SimpleTournamentParticipantView> members;
}
