package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

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
}
