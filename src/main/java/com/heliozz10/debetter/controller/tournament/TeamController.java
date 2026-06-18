package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.team.in.TeamFormDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateParticipantDto;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.service.tournament.TeamService;
import com.heliozz10.debetter.service.tournament.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public PageableResult<TeamView> getTeamsByTournamentId(
            @PathVariable Long tournamentId,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Team> teams = teamService.getTeamsByTournamentId(tournamentId, pageable);
        return new PageableResult<>(
                teamService.toTeamViews(teams.getContent()),
                teams.getTotalElements(),
                teams.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public TeamView getTeamByTournamentIdAndId(@PathVariable Long tournamentId, @PathVariable Long id) {
        return teamService.toTeamView(teamService.getTeamByTournamentIdAndId(tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'PARTICIPANT'")
    @PostMapping
    public void registerTeamToTournament(@PathVariable Long tournamentId, @Valid @RequestBody TeamFormDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        tournamentService.registerTeamToTournament(dto, tournamentId, profile.getId());
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{id}/organizer-update")
    public void updateTeam_Organizer(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody TeamUpdateOrganizerDto dto) {
        teamService.updateTeam_Organizer(dto, tournamentId, id);
    }

    @PreAuthorize("principal.role.name() == 'PARTICIPANT' and @tournamentSecurity.hasViewPermission(principal, #tournamentId)")
    @PatchMapping("/{id}/participant-update")
    public void updateTeam_Participant(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody TeamUpdateParticipantDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ParticipantProfile profile = (ParticipantProfile) user.getProfile();
        teamService.updateTeam_Participant(dto, tournamentId, id, profile.getId());
    }
}
