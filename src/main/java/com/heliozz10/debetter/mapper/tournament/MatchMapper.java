package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import com.heliozz10.debetter.dto.tournament.match.out.ParticipantScoreView;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.service.tournament.MatchParticipantScorePolicy;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
                TournamentParticipantMapper.class,
                JudgeMapper.class
        }
)
public interface MatchMapper {
    @Mapping(target = "participantScores", ignore = true)
    void receiveMatchResult(MatchResultDto dto, @MappingTarget Match match);

    @Mapping(target = "team1ParticipantScores", ignore = true)
    @Mapping(target = "team2ParticipantScores", ignore = true)
    @Mapping(target = "team3ParticipantScores", ignore = true)
    @Mapping(target = "team4ParticipantScores", ignore = true)
    @Mapping(target = "participantScoresComplete", ignore = true)
    @Mapping(target = "participantScoresRepairable", ignore = true)
    MatchView toMappedMatchView(Match match);

    default MatchView toMatchView(Match match) {
        return toMatchView(match, false);
    }

    default List<MatchView> toMatchViews(List<Match> matches) {
        return toMatchViews(matches, false);
    }

    @AfterMapping
    default void addParticipantScoreMetadata(Match match, @MappingTarget MatchView view) {
        if (view.getDebater1() != null) {
            view.getDebater1().setSpeakerScore(null);
        }
        if (view.getDebater2() != null) {
            view.getDebater2().setSpeakerScore(null);
        }

        if (!MatchParticipantScorePolicy.isTeamFormat(match)
                || !MatchParticipantScorePolicy.isPreliminaryMatch(match)
                || MatchParticipantScorePolicy.expectedParticipants(match).isEmpty()) {
            view.setParticipantScoresComplete(null);
            view.setParticipantScoresRepairable(null);
            return;
        }

        view.setParticipantScoresComplete(MatchParticipantScorePolicy.hasCompleteParticipantScores(match));
        view.setParticipantScoresRepairable(MatchParticipantScorePolicy.isRepairable(match));
    }

    default MatchView toMatchView(Match match, boolean includeExactResults) {
        MatchView view = toMappedMatchView(match);
        if (view == null) {
            return null;
        }

        if (includeExactResults) {
            if (view.getDebater1() != null && match.getDebater1() != null) {
                view.getDebater1().setSpeakerScore(match.getDebater1().getSpeakerScore());
            }
            if (view.getDebater2() != null && match.getDebater2() != null) {
                view.getDebater2().setSpeakerScore(match.getDebater2().getSpeakerScore());
            }
            view.setDebater1Score(match.getDebater1Score());
            view.setDebater2Score(match.getDebater2Score());
            view.setTeam1ParticipantScores(participantScoresForTeam(match.getTeam1(), match.getParticipantScores()));
            view.setTeam2ParticipantScores(participantScoresForTeam(match.getTeam2(), match.getParticipantScores()));
            view.setTeam3ParticipantScores(participantScoresForTeam(match.getTeam3(), match.getParticipantScores()));
            view.setTeam4ParticipantScores(participantScoresForTeam(match.getTeam4(), match.getParticipantScores()));
        } else {
            redactPrivateResults(view);
        }
        return view;
    }

    default List<MatchView> toMatchViews(List<Match> matches, boolean includeExactResults) {
        return matches == null ? List.of() : matches.stream()
                .map(match -> toMatchView(match, includeExactResults))
                .toList();
    }

    private void redactPrivateResults(MatchView view) {
        view.setTeam1Score(null);
        view.setTeam2Score(null);
        view.setTeam3Score(null);
        view.setTeam4Score(null);
        view.setTeam1Won(null);
        view.setTeam2Won(null);
        view.setTeam3Won(null);
        view.setTeam4Won(null);
        view.setDebater1Score(null);
        view.setDebater2Score(null);
        view.setWinnerParticipantId(null);
        view.setTeam1ParticipantScores(null);
        view.setTeam2ParticipantScores(null);
        view.setTeam3ParticipantScores(null);
        view.setTeam4ParticipantScores(null);
        redactParticipant(view.getDebater1());
        redactParticipant(view.getDebater2());
        redactJudge(view.getJudge());
    }

    private void redactParticipant(SimpleTournamentParticipantView participant) {
        if (participant == null) {
            return;
        }

        participant.setSpeakerScore(null);
        participant.setParticipantProfile(null);
        SimpleUserView user = participant.getUser();
        if (user != null) {
            user.setId(null);
            user.setUsername(null);
            user.setRole(null);
        }
    }

    private void redactJudge(JudgeView judge) {
        if (judge == null) {
            return;
        }

        judge.setPhoneNumber(null);
        judge.setEmail(null);
        judge.setSocialProfiles(null);
        judge.setCheckedIn(null);
    }

    private List<ParticipantScoreView> participantScoresForTeam(Team team, List<MatchParticipantScore> scores) {
        if(team == null || team.getId() == null) {
            return List.of();
        }

        return safeScores(scores).stream()
                .filter(score -> score.getParticipant() != null && score.getParticipant().getTeam() != null)
                .filter(score -> Objects.equals(team.getId(), score.getParticipant().getTeam().getId()))
                .map(score -> new ParticipantScoreView(score.getParticipant().getId(), score.getScore()))
                .toList();
    }

    private List<MatchParticipantScore> safeScores(List<MatchParticipantScore> scores) {
        return scores == null ? List.of() : scores;
    }
}
