package com.heliozz10.debetter.service.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundGroupServiceTest {
    @Mock
    private RoundGroupRepository roundGroupRepository;
    @Mock
    private RoundService roundService;
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private TeamRepository teamRepository;

    @Test
    void completingPreliminariesSeedsAndGeneratesBothKnockoutBrackets() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setStarted(true);

        Team team1 = team(501L, 601L, 120);
        Team team2 = team(502L, 602L, 110);
        tournament.setTeams(List.of(team1, team2));

        RoundGroup preliminary = group(101L, tournament, RoundGroupType.PRELIMINARY, 1);
        Round currentRound = round(201L, preliminary, 1);
        Match completedMatch = completedPreliminaryMatch(team1, team2);
        completedMatch.setRound(currentRound);
        currentRound.setMatches(List.of(completedMatch));
        preliminary.setRounds(List.of(currentRound));

        RoundGroup teamElimination = group(102L, tournament, RoundGroupType.TEAM_ELIMINATION, null);
        Round teamFirstRound = round(202L, teamElimination, 1);
        teamElimination.setRounds(List.of(teamFirstRound));

        RoundGroup soloElimination = group(103L, tournament, RoundGroupType.SOLO_ELIMINATION, null);
        soloElimination.setFormat(DebateFormat.LD);
        Round soloFirstRound = round(203L, soloElimination, 1);
        soloElimination.setRounds(List.of(soloFirstRound));
        tournament.setRoundGroups(List.of(preliminary, teamElimination, soloElimination));

        when(roundGroupRepository.findFullByTournamentIdAndId(53L, 101L)).thenReturn(Optional.of(preliminary));
        when(roundRepository.findWithTeamsByRoundGroup_IdAndRoundNumber(101L, 1)).thenReturn(Optional.of(currentRound));
        when(roundRepository.areAllMatchesCompleted(currentRound)).thenReturn(true);
        when(roundRepository.findByRoundGroup_IdAndRoundNumber(102L, 1)).thenReturn(Optional.of(teamFirstRound));
        when(roundRepository.findByRoundGroup_IdAndRoundNumber(103L, 1)).thenReturn(Optional.of(soloFirstRound));
        when(teamRepository.findByTournamentAndDisqualifiedFalse(tournament)).thenReturn(List.of(team1, team2));

        new RoundGroupService(roundGroupRepository, roundService, roundRepository, teamRepository)
                .proceedToNextRound(53L, 101L);

        verify(roundService).setTeams(teamFirstRound, List.of(team1, team2));
        verify(roundService).setDebaters(soloFirstRound, List.of(team1.getMembers().getFirst(), team2.getMembers().getFirst()));
        verify(roundService).generateMatchesAndAssignJudges(teamFirstRound);
        verify(roundService).generateMatchesAndAssignJudges(soloFirstRound);
        verify(roundGroupRepository).save(teamElimination);
        verify(roundGroupRepository).save(soloElimination);
    }

    @Test
    void completingPreliminariesStartsTeamBracketWhenLdIsNotConfigured() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setStarted(true);

        Team team1 = team(501L, 601L, 120);
        Team team2 = team(502L, 602L, 110);
        Team team3 = team(503L, 603L, 100);
        Team team4 = team(504L, 604L, 90);
        List<Team> teams = List.of(team1, team2, team3, team4);
        tournament.setTeams(teams);

        RoundGroup preliminary = group(101L, tournament, RoundGroupType.PRELIMINARY, 1);
        preliminary.setFormat(DebateFormat.BPF);
        Round currentRound = round(201L, preliminary, 1);
        preliminary.setRounds(List.of(currentRound));

        RoundGroup teamElimination = group(102L, tournament, RoundGroupType.TEAM_ELIMINATION, null);
        teamElimination.setFormat(DebateFormat.BPF);
        Round teamFirstRound = round(202L, teamElimination, 1);
        teamElimination.setRounds(List.of(teamFirstRound));
        tournament.setRoundGroups(List.of(preliminary, teamElimination));

        when(roundGroupRepository.findFullByTournamentIdAndId(53L, 101L)).thenReturn(Optional.of(preliminary));
        when(roundRepository.findWithTeamsByRoundGroup_IdAndRoundNumber(101L, 1)).thenReturn(Optional.of(currentRound));
        when(roundRepository.areAllMatchesCompleted(currentRound)).thenReturn(true);
        when(roundRepository.findByRoundGroup_IdAndRoundNumber(102L, 1)).thenReturn(Optional.of(teamFirstRound));
        when(teamRepository.findByTournamentAndDisqualifiedFalse(tournament)).thenReturn(teams);

        new RoundGroupService(roundGroupRepository, roundService, roundRepository, teamRepository)
                .proceedToNextRound(53L, 101L);

        verify(roundService).setTeams(teamFirstRound, teams);
        verify(roundService).generateMatchesAndAssignJudges(teamFirstRound);
        verify(roundGroupRepository).save(teamElimination);
    }

    @Test
    void bpfEliminationProgressionAdvancesFourWinnersIntoTheFinal() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setStarted(true);

        RoundGroup elimination = group(102L, tournament, RoundGroupType.TEAM_ELIMINATION, 1);
        elimination.setFormat(DebateFormat.BPF);
        Round currentRound = round(202L, elimination, 1);
        Round finalRound = round(203L, elimination, 2);
        elimination.setRounds(List.of(currentRound, finalRound));
        tournament.setRoundGroups(List.of(elimination));

        List<Team> winners = List.of(
                team(501L, 601L, 120),
                team(502L, 602L, 110),
                team(503L, 603L, 100),
                team(504L, 604L, 90)
        );
        when(roundGroupRepository.findFullByTournamentIdAndId(53L, 102L)).thenReturn(Optional.of(elimination));
        when(roundRepository.findWithTeamsByRoundGroup_IdAndRoundNumber(102L, 1)).thenReturn(Optional.of(currentRound));
        when(roundRepository.areAllMatchesCompleted(currentRound)).thenReturn(true);
        when(roundRepository.findByRoundGroup_IdAndRoundNumber(102L, 2)).thenReturn(Optional.of(finalRound));
        when(roundService.getMatchWinnerTeams(202L)).thenReturn(winners);

        new RoundGroupService(roundGroupRepository, roundService, roundRepository, teamRepository)
                .proceedToNextRound(53L, 102L);

        verify(roundService).setTeams(finalRound, winners);
        verify(roundService).generateMatchesAndAssignJudges(finalRound);
        verify(roundGroupRepository).save(elimination);
        assertEquals(2, elimination.getCurrentRoundNumber());
    }

    @Test
    void completingPreliminariesBlocksLdGenerationWhenAnyMatchSpeakerScoreIsMissing() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setStarted(true);

        Team team1 = team(501L, 601L, 120);
        Team team2 = team(502L, 602L, 110);
        tournament.setTeams(List.of(team1, team2));

        RoundGroup preliminary = group(101L, tournament, RoundGroupType.PRELIMINARY, 1);
        Round currentRound = round(201L, preliminary, 1);
        Match incompleteScores = completedPreliminaryMatch(team1, team2);
        incompleteScores.setRound(currentRound);
        incompleteScores.setParticipantScores(List.of(incompleteScores.getParticipantScores().getFirst()));
        currentRound.setMatches(List.of(incompleteScores));
        preliminary.setRounds(List.of(currentRound));

        RoundGroup teamElimination = group(102L, tournament, RoundGroupType.TEAM_ELIMINATION, null);
        teamElimination.setRounds(List.of(round(202L, teamElimination, 1)));
        RoundGroup soloElimination = group(103L, tournament, RoundGroupType.SOLO_ELIMINATION, null);
        soloElimination.setFormat(DebateFormat.LD);
        soloElimination.setRounds(List.of(round(203L, soloElimination, 1)));
        tournament.setRoundGroups(List.of(preliminary, teamElimination, soloElimination));

        when(roundGroupRepository.findFullByTournamentIdAndId(53L, 101L)).thenReturn(Optional.of(preliminary));
        when(roundRepository.findWithTeamsByRoundGroup_IdAndRoundNumber(101L, 1)).thenReturn(Optional.of(currentRound));
        when(roundRepository.areAllMatchesCompleted(currentRound)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new RoundGroupService(roundGroupRepository, roundService, roundRepository, teamRepository)
                        .proceedToNextRound(53L, 101L)
        );

        assertEquals("Cannot generate LD bracket while preliminary speaker points are missing.", exception.getMessage());
        verifyNoInteractions(roundService, teamRepository);
    }

    private static RoundGroup group(Long id, Tournament tournament, RoundGroupType type, Integer currentRoundNumber) {
        RoundGroup group = new RoundGroup();
        group.setId(id);
        group.setTournament(tournament);
        group.setType(type);
        group.setFormat(DebateFormat.APF);
        group.setCurrentRoundNumber(currentRoundNumber);
        return group;
    }

    private static Round round(Long id, RoundGroup group, int roundNumber) {
        Round round = new Round();
        round.setId(id);
        round.setRoundGroup(group);
        round.setRoundNumber(roundNumber);
        return round;
    }

    private static Team team(Long teamId, Long participantId, int speakerScore) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setId(participantId);
        participant.setSpeakerScore(speakerScore);
        Team team = new Team();
        team.setId(teamId);
        team.setPreliminaryScore(speakerScore);
        team.setMembers(List.of(participant));
        return team;
    }

    private static Match completedPreliminaryMatch(Team... teams) {
        Match match = new Match();
        match.setCompleted(true);
        if(teams.length > 0) {
            match.setTeam1(teams[0]);
            match.setTeam1Score(teams[0].getPreliminaryScore());
            match.setTeam1Won(true);
        }
        if(teams.length > 1) {
            match.setTeam2(teams[1]);
            match.setTeam2Score(teams[1].getPreliminaryScore());
            match.setTeam2Won(false);
        }
        if(teams.length > 2) {
            match.setTeam3(teams[2]);
            match.setTeam3Score(teams[2].getPreliminaryScore());
            match.setTeam3Won(true);
        }
        if(teams.length > 3) {
            match.setTeam4(teams[3]);
            match.setTeam4Score(teams[3].getPreliminaryScore());
            match.setTeam4Won(false);
        }
        List<MatchParticipantScore> scores = java.util.Arrays.stream(teams)
                .flatMap(team -> team.getMembers().stream())
                .map(participant -> {
                    MatchParticipantScore score = new MatchParticipantScore();
                    score.setMatch(match);
                    score.setParticipant(participant);
                    score.setScore(participant.getSpeakerScore());
                    return score;
                })
                .toList();
        match.setParticipantScores(scores);
        return match;
    }
}
