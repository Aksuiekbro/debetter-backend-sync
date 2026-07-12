package com.heliozz10.debetter.dto.tournament.match.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer debater1Score;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer debater2Score;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParticipantScoreView> team1ParticipantScores;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParticipantScoreView> team2ParticipantScores;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParticipantScoreView> team3ParticipantScores;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParticipantScoreView> team4ParticipantScores;
    private Boolean participantScoresComplete;
    private Boolean participantScoresRepairable;
    private Boolean team1Won;
    private Boolean team2Won;
    private Boolean team3Won;
    private Boolean team4Won;
    private Boolean completed;
}
