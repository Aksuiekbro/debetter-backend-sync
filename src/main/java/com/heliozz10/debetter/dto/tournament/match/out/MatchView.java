package com.heliozz10.debetter.dto.tournament.match.out;

import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchView {
    private Long id;
    private SimpleTeamView team1;
    private SimpleTeamView team2;
    private SimpleTeamView team3;
    private SimpleTeamView team4;
    private SimpleTournamentParticipantView debater1;
    private SimpleTournamentParticipantView debater2;
    private String location;
    private LocalDateTime startTime;
    private JudgeView judge;
    private Integer team1Score;
    private Integer team2Score;
    private Integer team3Score;
    private Integer team4Score;
    private Integer debater1Score;
    private Integer debater2Score;
    private Boolean completed;
}
