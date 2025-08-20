package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.team.in.TeamFormDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateParticipantDto;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.service.tournament.TeamService;
import com.heliozz10.debetter.service.tournament.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/teams")
public class TeamController {
    private final TeamService teamService;
    private final TeamMapper teamMapper;

    private final TournamentService tournamentService;

    @GetMapping
    public PageableResult<SimpleTeamView> getTeamsByTournamentId(
            @PathVariable Long tournamentId,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Team> teams = teamService.getTeamsByTournamentId(tournamentId, pageable);
        return new PageableResult<>(
                teamMapper.toSimpleTeamViews(teams.getContent()),
                teams.getTotalElements(),
                teams.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public TeamView getTeamByTournamentIdAndId(@PathVariable Long tournamentId, @PathVariable Long id) {
        return teamMapper.toTeamView(teamService.getTeamByTournamentIdAndId(id));
    }

    @PostMapping
    public void registerTeamToTournament(@PathVariable Long tournamentId, @RequestBody TeamFormDto dto) {
        tournamentService.registerTeamToTournament(dto, tournamentId);
    }

    @PatchMapping("/{id}/organizer-update")
    public void updateTeam_Organizer(@PathVariable Long tournamentId, @PathVariable Long id, @RequestBody TeamUpdateOrganizerDto dto) {
        teamService.updateTeam_Organizer(dto, tournamentId, id);
    }

    @PatchMapping("/{id}/participant-update")
    public void updateTeam_Participant(@PathVariable Long tournamentId, @PathVariable Long id, @RequestBody TeamUpdateParticipantDto dto) {
        teamService.updateTeam_Participant(dto, tournamentId, id);
    }

    @DeleteMapping("/{id}")
    public void removeTeamFromTournament(@PathVariable Long tournamentId, @PathVariable Long id) {
        tournamentService.removeTeamFromTournament(tournamentId, id);
    }
}
