package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicTournamentReadAccessTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void publicTournamentPagesCanLoadNestedReadEndpointsWithoutLogin() throws Exception {
        Tournament tournament = tournamentRepository.save(publicTournament());

        RoundGroup roundGroup = new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.APF);
        roundGroup.setCurrentRoundNumber(1);
        roundGroup = roundGroupRepository.save(roundGroup);

        Round round = new Round(roundGroup, "Round 1", 1);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.save(round);

        String tournamentRoot = "/api/tournaments/" + tournament.getId();
        String roundGroupRoot = tournamentRoot + "/round-groups/" + roundGroup.getId();
        String roundRoot = roundGroupRoot + "/rounds/" + round.getId();

        mockMvc.perform(get(tournamentRoot + "/participants").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(tournamentRoot + "/teams").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(tournamentRoot + "/announcements").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(tournamentRoot + "/schedules").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(tournamentRoot + "/judges").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(tournamentRoot + "/round-groups").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(roundGroupRoot + "/rounds").servletPath("/api")).andExpect(status().isOk());
        mockMvc.perform(get(roundRoot + "/matches").servletPath("/api")).andExpect(status().isOk());
    }

    @Test
    void pairingStateRoundCanLoadWhenTeamsAndMatchesAlreadyExist() {
        Tournament tournament = tournamentRepository.save(publicTournament());

        RoundGroup roundGroup = new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.APF);
        roundGroup.setCurrentRoundNumber(1);
        roundGroup = roundGroupRepository.save(roundGroup);

        Round round = new Round(roundGroup, "Round 1", 1);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        Team firstTeam = teamRepository.save(team(tournament, "First Team"));
        Team secondTeam = teamRepository.save(team(tournament, "Second Team"));
        round.getTeams().addAll(List.of(firstTeam, secondTeam));

        Match match = new Match();
        match.setRound(round);
        match.setTeam1(firstTeam);
        match.setTeam2(secondTeam);
        match.setCompleted(false);
        match.setIsBye(false);
        round.getMatches().add(match);
        roundRepository.saveAndFlush(round);

        Long tournamentId = tournament.getId();
        Long roundGroupId = roundGroup.getId();
        Long roundId = round.getId();
        assertDoesNotThrow(() -> roundRepository
                .findWithPairingStateByTournamentAndRoundGroupAndId(tournamentId, roundGroupId, roundId)
                .orElseThrow());
    }

    private static Tournament publicTournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Public Cup");
        tournament.setDescription("Public read smoke test");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    private static Team team(Tournament tournament, String name) {
        Team team = new Team();
        team.setName(name);
        team.setTournament(tournament);
        team.setActive(true);
        team.setCheckedIn(true);
        team.setDisqualified(false);
        return team;
    }
}
