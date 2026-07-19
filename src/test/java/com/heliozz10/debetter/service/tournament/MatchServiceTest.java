package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.match.in.MatchLocationDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchUpdateDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.ParticipantScoreDto;
import com.heliozz10.debetter.dto.tournament.match.in.TeamResultDto;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchParticipantScoreRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private JudgeRepository judgeRepository;

    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Mock
    private MatchParticipantScoreRepository matchParticipantScoreRepository;

    @Mock
    private TournamentSecurity tournamentSecurity;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                roundRepository,
                teamRepository,
                judgeRepository,
                tournamentParticipantRepository,
                matchParticipantScoreRepository,
                tournamentSecurity,
                new ObjectMapper()
        );
    }

    @Test
    void anonymousUsersSeeEmptyMatchesWhenPairingsAreNotPublished() {
        Round round = new Round();
        round.setMatchesArePublic(false);
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));

        Page<Match> matches = matchService.getVisibleMatchesByRoundId(53L, 101L, 201L, null, PageRequest.of(0, 10));

        assertEquals(0, matches.getTotalElements());
        verify(matchRepository, never()).findByRoundId(201L, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")));
    }

    @Test
    void anonymousUsersCanSeeMatchesAfterPairingsArePublished() {
        Round round = new Round();
        round.setMatchesArePublic(true);
        Match match = new Match();
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));
        when(matchRepository.findByRoundId(201L, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"))))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<Match> matches = matchService.getVisibleMatchesByRoundId(53L, 101L, 201L, null, PageRequest.of(0, 10));

        assertEquals(1, matches.getTotalElements());
    }

    @Test
    void submitMatchResultsRejectsMatchesOutsideRound() {
        List<MatchResultDto> results = List.of(
                new MatchResultDto(301L, List.of(), null),
                new MatchResultDto(999L, List.of(), null)
        );
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L, 999L))).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Some match results do not belong to the specified round.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsRejectsACompletedLookingBallotThatOmitsOneTeam() {
        Match match = teamMatch(301L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(new TeamResultDto(501L, true, List.of(new ParticipantScoreDto(601L, 75)))),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("A result is required for every team in the match.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsNewTeamBallotUpdatesMatchAndCumulativeSpeakerTotals() {
        Match match = teamMatch(301L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        new TeamResultDto(501L, true, List.of(new ParticipantScoreDto(601L, 75))),
                        new TeamResultDto(502L, false, List.of(new ParticipantScoreDto(602L, 70)))
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        verify(matchRepository).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(tournamentParticipantRepository).addSpeakerScore(601L, 75);
        verify(tournamentParticipantRepository).addSpeakerScore(602L, 70);
    }

    @Test
    void submitMatchResultsLegacyRepairPersistsSpeakerRowsWithoutChangingCumulativeScores() {
        Match match = teamMatchWithTwoMembers(301L);
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        teamResult(501L, true, 601L, 70, 603L, 71),
                        teamResult(502L, false, 602L, 72, 604L, 73)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));
        when(matchParticipantScoreRepository.countByMatchId(301L)).thenReturn(0L);

        matchService.submitMatchResults(53L, 101L, 201L, results);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchParticipantScore>> scoreCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchParticipantScoreRepository).saveAll(scoreCaptor.capture());
        assertEquals(
                List.of(70, 71, 72, 73),
                scoreCaptor.getValue().stream().map(MatchParticipantScore::getScore).toList()
        );
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsRejectsPartialParticipantRowsWithoutChangingAnything() {
        Match match = teamMatchWithTwoMembers(301L);
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        MatchParticipantScore existingScore = new MatchParticipantScore();
        existingScore.setMatch(match);
        existingScore.setParticipant(match.getTeam1().getMembers().getFirst());
        existingScore.setScore(70);
        match.setParticipantScores(List.of(existingScore));
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        teamResult(501L, true, 601L, 70, 603L, 71),
                        teamResult(502L, false, 602L, 72, 604L, 73)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));
        when(matchParticipantScoreRepository.countByMatchId(301L)).thenReturn(1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Cannot re-submit results for already completed matches.", exception.getMessage());
        assertEquals(1, match.getParticipantScores().size());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsRejectsContradictoryZeroRowRepairAtomically() {
        Match match = teamMatchWithTwoMembers(301L);
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        teamResult(501L, true, 601L, 70, 603L, 70),
                        teamResult(502L, false, 602L, 72, 604L, 73)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));
        when(matchParticipantScoreRepository.countByMatchId(301L)).thenReturn(0L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Legacy participant-score repair must preserve the completed team total and winner.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsRejectsSecondZeroRowRepairAfterTheFirstRepair() {
        Match match = teamMatchWithTwoMembers(301L);
        match.setCompleted(true);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        teamResult(501L, true, 601L, 70, 603L, 71),
                        teamResult(502L, false, 602L, 72, 604L, 73)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));
        when(matchParticipantScoreRepository.countByMatchId(301L)).thenReturn(0L, 4L);

        matchService.submitMatchResults(53L, 101L, 201L, results);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Cannot re-submit results for already completed matches.", exception.getMessage());
        verify(matchParticipantScoreRepository, times(1)).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsEliminationBallotPersistsOnlyWinLoseOutcomes() throws Exception {
        Match match = teamMatchWithTwoMembers(301L);
        match.getRound().getRoundGroup().setType(RoundGroupType.TEAM_ELIMINATION);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        new TeamResultDto(501L, true, null),
                        new TeamResultDto(502L, false, null)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(matchRepository).updateMatchScoresBulk(resultCaptor.capture());
        assertTrue(new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team1score").isNull());
        assertTrue(new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team2score").isNull());
        assertTrue(new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team1won").asBoolean());
        assertFalse(new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team2won").asBoolean());
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsBpfEliminationPersistsTwoWinnersWithoutSpeakerPoints() throws Exception {
        Match match = bpfMatchWithTwoMembers(302L);
        match.getRound().getRoundGroup().setType(RoundGroupType.TEAM_ELIMINATION);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                302L,
                List.of(
                        new TeamResultDto(501L, true, null),
                        new TeamResultDto(502L, true, null),
                        new TeamResultDto(503L, false, null),
                        new TeamResultDto(504L, false, null)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(302L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(302L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(matchRepository).updateMatchScoresBulk(resultCaptor.capture());
        var payload = new ObjectMapper().readTree(resultCaptor.getValue()).get(0);
        assertTrue(payload.get("team1won").asBoolean());
        assertTrue(payload.get("team2won").asBoolean());
        assertFalse(payload.get("team3won").asBoolean());
        assertFalse(payload.get("team4won").asBoolean());
        assertTrue(payload.get("team1score").isNull());
        assertTrue(payload.get("team4score").isNull());
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(tournamentParticipantRepository, never()).addSpeakerScore(anyLong(), anyInt());
    }

    @Test
    void submitMatchResultsPreliminaryBallotStillRequiresSpeakerPoints() {
        Match match = teamMatchWithTwoMembers(301L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        new TeamResultDto(501L, true, null),
                        new TeamResultDto(502L, false, null)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("A score is required for every participating debater.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsPersistsDistinctApfSpeakerScoresAlongsideTeamTotals() throws Exception {
        Match match = teamMatchWithTwoMembers(301L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                301L,
                List.of(
                        new TeamResultDto(501L, true, List.of(
                                new ParticipantScoreDto(601L, 70),
                                new ParticipantScoreDto(603L, 71)
                        )),
                        new TeamResultDto(502L, false, List.of(
                                new ParticipantScoreDto(602L, 72),
                                new ParticipantScoreDto(604L, 73)
                        ))
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchParticipantScore>> scoreCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchParticipantScoreRepository).saveAll(scoreCaptor.capture());
        assertEquals(
                List.of(70, 71, 72, 73),
                scoreCaptor.getValue().stream().map(MatchParticipantScore::getScore).toList()
        );
        assertEquals(
                List.of(601L, 603L, 602L, 604L),
                scoreCaptor.getValue().stream().map(score -> score.getParticipant().getId()).toList()
        );

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(matchRepository).updateMatchScoresBulk(resultCaptor.capture());
        assertEquals(141, new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team1score").asInt());
        assertEquals(145, new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team2score").asInt());
        assertEquals(true, new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team1won").asBoolean());
        assertEquals(false, new ObjectMapper().readTree(resultCaptor.getValue()).get(0).get("team2won").asBoolean());
    }

    @Test
    void submitMatchResultsAcceptsTwoBpfWinnersAndPersistsAllSpeakerScores() {
        Match match = bpfMatchWithTwoMembers(302L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                302L,
                List.of(
                        teamResult(501L, true, 601L, 70, 603L, 71),
                        teamResult(502L, true, 602L, 72, 604L, 73),
                        teamResult(503L, false, 605L, 74, 607L, 75),
                        teamResult(504L, false, 606L, 76, 608L, 77)
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(302L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(302L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchParticipantScore>> scoreCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchParticipantScoreRepository).saveAll(scoreCaptor.capture());
        assertEquals(8, scoreCaptor.getValue().size());
        verify(matchRepository).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsRejectsBpfBallotWithoutEveryTeam() {
        Match match = bpfMatch(302L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                302L,
                List.of(
                        new TeamResultDto(501L, true, List.of(new ParticipantScoreDto(601L, 75))),
                        new TeamResultDto(502L, false, List.of(new ParticipantScoreDto(602L, 70)))
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(302L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(302L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("A result is required for every team in the match.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsRejectsBpfBallotWithOnlyOneWinner() {
        Match match = bpfMatch(302L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                302L,
                List.of(
                        new TeamResultDto(501L, true, List.of(new ParticipantScoreDto(601L, 75))),
                        new TeamResultDto(502L, false, List.of(new ParticipantScoreDto(602L, 74))),
                        new TeamResultDto(503L, false, List.of(new ParticipantScoreDto(603L, 73))),
                        new TeamResultDto(504L, false, List.of(new ParticipantScoreDto(604L, 72)))
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(302L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(302L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Exactly 2 teams must be marked as winners.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsRejectsMissingWinnerFlag() {
        Match match = teamMatch(304L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                304L,
                List.of(
                        new TeamResultDto(501L, true, List.of(new ParticipantScoreDto(601L, 75))),
                        new TeamResultDto(502L, null, List.of(new ParticipantScoreDto(602L, 70)))
                ),
                null
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(304L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(304L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("Every team must have an explicit winner result.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void submitMatchResultsAcceptsOneExplicitLdWinnerWithoutSpeakerPoints() {
        Match match = ldMatch(303L);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                303L,
                null,
                null,
                701L
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(303L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(303L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        verify(matchRepository).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(matchRepository).updateWinnerParticipantId(303L, 701L);
        verify(matchParticipantScoreRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void submitMatchResultsPreliminaryLdStillAcceptsDistinctSpeakerPoints() {
        Match match = ldMatch(303L);
        match.getRound().getRoundGroup().setType(RoundGroupType.PRELIMINARY);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                303L,
                null,
                List.of(new ParticipantScoreDto(701L, 75), new ParticipantScoreDto(702L, 74))
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(303L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(303L))).thenReturn(List.of(match));

        matchService.submitMatchResults(53L, 101L, 201L, results);

        verify(matchRepository).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
        verify(matchRepository, never()).updateWinnerParticipantId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void submitMatchResultsPreliminaryLdStillRejectsTiedSpeakerPoints() {
        Match match = ldMatch(303L);
        match.getRound().getRoundGroup().setType(RoundGroupType.PRELIMINARY);
        List<MatchResultDto> results = List.of(new MatchResultDto(
                303L,
                null,
                List.of(new ParticipantScoreDto(701L, 75), new ParticipantScoreDto(702L, 75))
        ));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(303L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(303L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("LD results cannot be tied.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitMatchResultsRejectsLdWinnerOutsideTheMatch() {
        Match match = ldMatch(303L);
        List<MatchResultDto> results = List.of(new MatchResultDto(303L, null, null, 999L));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(303L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(303L))).thenReturn(List.of(match));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, 101L, 201L, results)
        );

        assertEquals("LD results must identify exactly one participating winner.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateMatchAllowsOrganizersToEditRoomJudgeAndTeamsInsideTheRound() {
        Match match = new Match();
        match.setId(301L);
        match.setCompleted(false);
        match.setLocation("Old room");
        Judge oldJudge = judge(401L);
        match.setJudge(oldJudge);
        Team oldTeam1 = team(501L);
        match.setTeam1(oldTeam1);

        Judge newJudge = judge(402L);
        Team newTeam1 = team(502L);
        Team newTeam2 = team(503L);

        MatchUpdateDto dto = new MatchUpdateDto();
        dto.setLocation("  Room B-12  ");
        dto.setJudgeId(402L);
        dto.setTeam1Id(502L);
        dto.setTeam2Id(503L);
        dto.setTeam3Id(null);

        when(matchRepository.findByTournamentRoundGroupRoundAndId(53L, 101L, 201L, 301L))
                .thenReturn(Optional.of(match));
        when(judgeRepository.findByTournamentIdAndId(53L, 402L)).thenReturn(Optional.of(newJudge));
        when(teamRepository.findByTournamentIdAndId(53L, 502L)).thenReturn(Optional.of(newTeam1));
        when(teamRepository.findByTournamentIdAndId(53L, 503L)).thenReturn(Optional.of(newTeam2));
        when(matchRepository.save(match)).thenReturn(match);

        Match updated = matchService.updateMatch(53L, 101L, 201L, 301L, dto);

        assertSame(match, updated);
        assertEquals("Room B-12", match.getLocation());
        assertSame(newJudge, match.getJudge());
        assertSame(newTeam1, match.getTeam1());
        assertSame(newTeam2, match.getTeam2());
        assertNull(match.getTeam3());
        assertNull(match.getTeam4());
        verify(matchRepository).save(match);
    }

    @Test
    void updateMatchRejectsMatchesOutsideTheSelectedTournamentRound() {
        MatchUpdateDto dto = new MatchUpdateDto();
        dto.setLocation("Room 1");
        when(matchRepository.findByTournamentRoundGroupRoundAndId(53L, 101L, 201L, 999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> matchService.updateMatch(53L, 101L, 201L, 999L, dto)
        );
        verify(matchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMatchRejectsJudgeOutsideTournament() {
        Match match = new Match();
        match.setCompleted(false);
        MatchUpdateDto dto = new MatchUpdateDto();
        dto.setJudgeId(999L);
        when(matchRepository.findByTournamentRoundGroupRoundAndId(53L, 101L, 201L, 301L))
                .thenReturn(Optional.of(match));
        when(judgeRepository.findByTournamentIdAndId(53L, 999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> matchService.updateMatch(53L, 101L, 201L, 301L, dto)
        );

        assertEquals("Judge not found in tournament", exception.getMessage());
        verify(matchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMatchRejectsDuplicateTeamSlots() {
        Match match = new Match();
        match.setCompleted(false);
        Team team = team(501L);
        MatchUpdateDto dto = new MatchUpdateDto();
        dto.setTeam1Id(501L);
        dto.setTeam2Id(501L);
        when(matchRepository.findByTournamentRoundGroupRoundAndId(53L, 101L, 201L, 301L))
                .thenReturn(Optional.of(match));
        when(teamRepository.findByTournamentIdAndId(53L, 501L)).thenReturn(Optional.of(team));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.updateMatch(53L, 101L, 201L, 301L, dto)
        );

        assertEquals("A team cannot occupy multiple slots in the same match", exception.getMessage());
        verify(matchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMatchLocationsTrimsLocationsAndClearsEmptyRooms() {
        Match firstMatch = new Match();
        firstMatch.setId(301L);
        firstMatch.setLocation("Old room");
        Match secondMatch = new Match();
        secondMatch.setId(302L);
        secondMatch.setLocation("Another room");
        List<MatchLocationDto> locations = List.of(
                new MatchLocationDto(301L, "  204  "),
                new MatchLocationDto(302L, "   ")
        );
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L, 302L))).thenReturn(2L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L, 302L))).thenReturn(List.of(firstMatch, secondMatch));

        matchService.updateMatchLocations(53L, 101L, 201L, locations);

        assertEquals("204", firstMatch.getLocation());
        assertNull(secondMatch.getLocation());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void updateMatchLocationsClearsRoomWithNull() {
        Match match = new Match();
        match.setId(301L);
        match.setLocation("Old room");
        List<MatchLocationDto> locations = List.of(new MatchLocationDto(301L, null));
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L))).thenReturn(1L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(match));

        matchService.updateMatchLocations(53L, 101L, 201L, locations);

        assertNull(match.getLocation());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void updateMatchLocationsRejectsInvalidBatchBeforeChangingAnyRoom() {
        Match match = new Match();
        match.setId(301L);
        match.setLocation("Original room");
        List<MatchLocationDto> locations = List.of(
                new MatchLocationDto(301L, "204"),
                new MatchLocationDto(999L, "205")
        );
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L, 999L))).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.updateMatchLocations(53L, 101L, 201L, locations)
        );

        assertEquals("Some matches do not belong to the specified round.", exception.getMessage());
        assertEquals("Original room", match.getLocation());
        verify(matchRepository, never()).findAllByIdForUpdate(org.mockito.ArgumentMatchers.anyList());
        verify(matchRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void updateMatchLocationsRejectsCompletedMatchBeforeChangingAnyRoom() {
        Match firstMatch = new Match();
        firstMatch.setId(301L);
        firstMatch.setCompleted(false);
        firstMatch.setLocation("Original room");
        Match completedMatch = new Match();
        completedMatch.setId(302L);
        completedMatch.setCompleted(true);
        completedMatch.setLocation("Completed room");
        List<MatchLocationDto> locations = List.of(
                new MatchLocationDto(301L, "204"),
                new MatchLocationDto(302L, "205")
        );
        when(matchRepository.countMatchesInRound(53L, 101L, 201L, List.of(301L, 302L))).thenReturn(2L);
        when(matchRepository.findAllByIdForUpdate(List.of(301L, 302L))).thenReturn(List.of(firstMatch, completedMatch));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.updateMatchLocations(53L, 101L, 201L, locations)
        );

        assertEquals("Cannot edit a completed match", exception.getMessage());
        assertEquals("Original room", firstMatch.getLocation());
        assertEquals("Completed room", completedMatch.getLocation());
        verify(matchRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyCollection());
    }

    private static Judge judge(Long id) {
        Judge judge = new Judge();
        judge.setId(id);
        judge.setFullName("Judge " + id);
        return judge;
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Team " + id);
        return team;
    }

    private static Match teamMatch(Long id) {
        Team team1 = team(501L);
        team1.setMembers(List.of(debater(601L)));
        Team team2 = team(502L);
        team2.setMembers(List.of(debater(602L)));
        com.heliozz10.debetter.content.tournament.round.RoundGroup group = new com.heliozz10.debetter.content.tournament.round.RoundGroup();
        group.setFormat(com.heliozz10.debetter.content.tournament.DebateFormat.APF);
        group.setType(RoundGroupType.PRELIMINARY);
        Round round = new Round();
        round.setRoundGroup(group);
        Match match = new Match();
        match.setId(id);
        match.setRound(round);
        match.setTeam1(team1);
        match.setTeam2(team2);
        return match;
    }

    private static Match bpfMatch(Long id) {
        Match match = teamMatch(id);
        match.getRound().getRoundGroup().setFormat(com.heliozz10.debetter.content.tournament.DebateFormat.BPF);
        Team team3 = team(503L);
        team3.setMembers(List.of(debater(603L)));
        Team team4 = team(504L);
        team4.setMembers(List.of(debater(604L)));
        match.setTeam3(team3);
        match.setTeam4(team4);
        return match;
    }

    private static Match teamMatchWithTwoMembers(Long id) {
        Match match = teamMatch(id);
        match.getTeam1().setMembers(List.of(debater(601L), debater(603L)));
        match.getTeam2().setMembers(List.of(debater(602L), debater(604L)));
        return match;
    }

    private static Match bpfMatchWithTwoMembers(Long id) {
        Match match = teamMatchWithTwoMembers(id);
        match.getRound().getRoundGroup().setFormat(com.heliozz10.debetter.content.tournament.DebateFormat.BPF);
        Team team3 = team(503L);
        team3.setMembers(List.of(debater(605L), debater(607L)));
        Team team4 = team(504L);
        team4.setMembers(List.of(debater(606L), debater(608L)));
        match.setTeam3(team3);
        match.setTeam4(team4);
        return match;
    }

    private static TeamResultDto teamResult(
            Long teamId,
            boolean won,
            Long firstParticipantId,
            int firstScore,
            Long secondParticipantId,
            int secondScore
    ) {
        return new TeamResultDto(
                teamId,
                won,
                List.of(
                        new ParticipantScoreDto(firstParticipantId, firstScore),
                        new ParticipantScoreDto(secondParticipantId, secondScore)
                )
        );
    }

    private static Match ldMatch(Long id) {
        com.heliozz10.debetter.content.tournament.round.RoundGroup group = new com.heliozz10.debetter.content.tournament.round.RoundGroup();
        group.setType(RoundGroupType.SOLO_ELIMINATION);
        group.setFormat(com.heliozz10.debetter.content.tournament.DebateFormat.LD);
        Round round = new Round();
        round.setRoundGroup(group);
        Match match = new Match();
        match.setId(id);
        match.setRound(round);
        match.setDebater1(debater(701L));
        match.setDebater2(debater(702L));
        return match;
    }

    private static TournamentParticipant debater(Long id) {
        TournamentParticipant debater = new TournamentParticipant();
        debater.setId(id);
        return debater;
    }
}
