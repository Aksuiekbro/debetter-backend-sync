package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private TournamentSecurity tournamentSecurity;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(matchRepository, roundRepository, tournamentSecurity, new ObjectMapper());
    }

    @Test
    void anonymousUsersSeeEmptyMatchesWhenPairingsAreNotPublished() {
        Round round = new Round();
        round.setMatchesArePublic(false);
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));

        Page<Match> matches = matchService.getVisibleMatchesByRoundId(53L, 101L, 201L, null, PageRequest.of(0, 10));

        assertEquals(0, matches.getTotalElements());
        verify(matchRepository, never()).findByRoundId(201L, PageRequest.of(0, 10));
    }

    @Test
    void anonymousUsersCanSeeMatchesAfterPairingsArePublished() {
        Round round = new Round();
        round.setMatchesArePublic(true);
        Match match = new Match();
        when(roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(53L, 101L, 201L))
                .thenReturn(Optional.of(round));
        when(matchRepository.findByRoundId(201L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<Match> matches = matchService.getVisibleMatchesByRoundId(53L, 101L, 201L, null, PageRequest.of(0, 10));

        assertEquals(1, matches.getTotalElements());
    }

    @Test
    void submitMatchResultsRejectsMatchesOutsideTournament() {
        List<MatchResultDto> results = List.of(
                new MatchResultDto(301L, List.of(), null),
                new MatchResultDto(999L, List.of(), null)
        );
        when(matchRepository.countMatchesInTournament(53L, List.of(301L, 999L))).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.submitMatchResults(53L, results)
        );

        assertEquals("Some match results do not belong to this tournament.", exception.getMessage());
        verify(matchRepository, never()).updateMatchScoresBulk(org.mockito.ArgumentMatchers.anyString());
    }
}
