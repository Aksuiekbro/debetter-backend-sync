package com.heliozz10.debetter.service.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.mapper.tournament.round.RoundMapper;
import com.heliozz10.debetter.repository.tournament.match.DebaterMatchupHistoryRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.match.TeamMatchupHistoryRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private RoundMapper roundMapper;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private TeamMatchupHistoryRepository teamMatchupHistoryRepository;
    @Mock
    private DebaterMatchupHistoryRepository debaterMatchupHistoryRepository;

    private RoundService roundService;

    @BeforeEach
    void setUp() {
        roundService = new RoundService(
                roundRepository,
                roundMapper,
                matchRepository,
                teamMatchupHistoryRepository,
                debaterMatchupHistoryRepository
        );
    }

    @Test
    void publishMatchesRequiresAtLeastOneMatch() {
        Round round = pairingRound();
        round.setMatches(new ArrayList<>());
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> roundService.publishMatches(53L, 101L, 201L)
        );

        assertEquals("Cannot publish pairings before matches are generated", exception.getMessage());
    }

    @Test
    void clearMatchesRejectsCompletedMatches() {
        Round round = pairingRound();
        Match completed = new Match();
        completed.setCompleted(true);
        round.setMatches(new ArrayList<>(List.of(completed)));
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> roundService.clearMatches(53L, 101L, 201L)
        );

        assertEquals("Cannot clear matches after results are submitted", exception.getMessage());
    }

    @Test
    void clearMatchesRemovesUncompletedMatchesAndUnpublishesRound() {
        Round round = pairingRound();
        Match match = new Match();
        match.setCompleted(false);
        round.setMatches(new ArrayList<>(List.of(match)));
        round.setMatchesArePublic(true);
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));

        roundService.clearMatches(53L, 101L, 201L);

        assertEquals(0, round.getMatches().size());
        assertFalse(round.getMatchesArePublic());
        verify(roundRepository).flush();
    }

    @Test
    void regenerateMatchesClearsOldUncompletedMatchesAndKeepsPairingsUnpublished() {
        Round round = pairingRound();
        Match oldMatch = new Match();
        oldMatch.setCompleted(false);
        round.setMatches(new ArrayList<>(List.of(oldMatch)));
        round.setMatchesArePublic(true);
        when(roundRepository.findWithPairingStateByTournamentAndRoundGroupAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));
        when(teamMatchupHistoryRepository.findByTeam1InAndTeam2In(anyList(), anyList()))
                .thenReturn(List.of());

        roundService.regenerateMatches(53L, 101L, 201L);

        assertEquals(0, round.getMatches().size());
        assertFalse(round.getMatchesArePublic());
        verify(roundRepository).flush();
        ArgumentCaptor<List<Match>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(round, captor.getValue().get(0).getRound());
        verify(roundRepository).assignJudgesForRound(201L);
    }

    private static Round pairingRound() {
        RoundGroup roundGroup = new RoundGroup();
        roundGroup.setId(101L);
        roundGroup.setType(RoundGroupType.PRELIMINARY);
        roundGroup.setFormat(DebateFormat.APF);

        Round round = new Round();
        round.setId(201L);
        round.setRoundGroup(roundGroup);
        round.setRoundNumber(1);
        round.setTeams(new ArrayList<>(List.of(team(1L), team(2L))));
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round.setMatchesArePublic(false);
        return round;
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }
}
