package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchParticipantScorePolicyTest {
    @Test
    void completedZeroRowTeamMatchIsRepairableWhenAggregatesAreComplete() {
        Match match = apfMatch();
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);

        assertFalse(MatchParticipantScorePolicy.hasCompleteParticipantScores(match));
        assertTrue(MatchParticipantScorePolicy.isRepairable(match));
    }

    @Test
    void partialRowsAreIncompleteAndNeverRepairable() {
        Match match = apfMatch();
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        match.setParticipantScores(List.of(score(match, match.getTeam1().getMembers().getFirst(), 70)));

        assertFalse(MatchParticipantScorePolicy.hasCompleteParticipantScores(match));
        assertFalse(MatchParticipantScorePolicy.isRepairable(match));
    }

    @Test
    void zeroRowsWithoutAggregateWinnersOrTotalsAreNotRepairable() {
        Match match = apfMatch();
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);

        assertFalse(MatchParticipantScorePolicy.isRepairable(match));
    }

    @Test
    void unusedTeamAggregateSlotsMakeLegacyRepairInvalid() {
        Match match = apfMatch();
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        match.setTeam3Score(1);
        match.setTeam3Won(true);

        assertFalse(MatchParticipantScorePolicy.hasValidWinnerFlags(match));
        assertFalse(MatchParticipantScorePolicy.isRepairable(match));
    }

    @Test
    void exactRowsAreCompleteAndNotRepairable() {
        Match match = apfMatch();
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        match.setParticipantScores(List.of(
                score(match, match.getTeam1().getMembers().getFirst(), 70),
                score(match, match.getTeam2().getMembers().getFirst(), 145)
        ));

        assertTrue(MatchParticipantScorePolicy.hasCompleteParticipantScores(match));
        assertFalse(MatchParticipantScorePolicy.isRepairable(match));
    }

    private static Match apfMatch() {
        RoundGroup group = new RoundGroup();
        group.setType(RoundGroupType.PRELIMINARY);
        group.setFormat(DebateFormat.APF);
        Round round = new Round();
        round.setRoundGroup(group);

        Team team1 = team(501L, 601L);
        Team team2 = team(502L, 602L);
        Match match = new Match();
        match.setId(301L);
        match.setRound(round);
        match.setTeam1(team1);
        match.setTeam2(team2);
        return match;
    }

    private static Team team(Long teamId, Long participantId) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setId(participantId);
        Team team = new Team();
        team.setId(teamId);
        team.setMembers(List.of(participant));
        participant.setTeam(team);
        return team;
    }

    private static MatchParticipantScore score(Match match, TournamentParticipant participant, int value) {
        MatchParticipantScore score = new MatchParticipantScore();
        score.setMatch(match);
        score.setParticipant(participant);
        score.setScore(value);
        return score;
    }
}
