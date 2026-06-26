package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.match.in.MatchUpdateDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.persistence.EntityNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    private TeamRepository teamRepository;

    @Mock
    private JudgeRepository judgeRepository;

    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;

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

    private static TournamentParticipant debater(Long id) {
        TournamentParticipant debater = new TournamentParticipant();
        debater.setId(id);
        return debater;
    }
}
