package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.common.in.IdDto;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.DebateFormatDto;
import com.heliozz10.debetter.dto.tournament.in.TournamentFormDto;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentView;
import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.service.tournament.TournamentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO: fix mappers
@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;
    private final TournamentMapper tournamentMapper;

    private final UserMapper userMapper;

    @GetMapping
    public PageableResult<SimpleTournamentView> getTournaments(
            @ModelAttribute TournamentGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return new PageableResult<>(
                tournamentMapper.toSimpleTournamentViews(tournamentService.getTournaments(params, pageable).getContent()),
                tournamentService.getTournaments(params, pageable).getTotalElements(),
                tournamentService.getTournaments(params, pageable).getTotalPages()
        );
    }

    @GetMapping("/id")
    public TournamentView getTournamentById(Long id) {
        return tournamentMapper.toTournamentView(tournamentService.getTournamentById(id));
    }

    @PostMapping
    public TournamentView createTournament(@RequestBody TournamentFormDto dto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return tournamentMapper.toTournamentView(tournamentService.createTournament(dto, profile.getId()));
    }

    @PatchMapping("/{id}")
    public TournamentView updateTournament(@PathVariable Long id, @RequestBody TournamentFormDto dto) {
        return tournamentMapper.toTournamentView(tournamentService.updateTournament(dto, id));
    }

    @DeleteMapping("/{id}")
    public void deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
    }

    @GetMapping("/{id}/main-organizer")
    public UserView getMainOrganizer(@PathVariable Long id) {
        return userMapper.toUserView(tournamentService.getMainOrganizer(id).get());
    }

    @GetMapping("/{id}/organizers")
    public List<SimpleUserView> getOrganizers(@PathVariable Long id) {
        return userMapper.toSimpleUserViews(tournamentService.getOrganizers(id));
    }

    @DeleteMapping("/{id}/organizers/{organizerId}")
    public void removeOrganizerFromTournament(@PathVariable Long id, @PathVariable Long organizerId) {
        tournamentService.removeOrganizerFromTournament(id, organizerId);
    }

    @PatchMapping("/{id}/enable")
    public void enableTournament(@PathVariable Long id) {
        tournamentService.enableTournament(id);
    }

    @PatchMapping("/{id}/disable")
    public void disableTournament(@PathVariable Long id) {
        tournamentService.disableTournament(id);
    }

    @PatchMapping("{id}/teams/{teamId}/check-in")
    public void checkInTeam(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.checkInTeam(id, teamId);
    }

    @PatchMapping("{id}/teams/{teamId}/uncheck-in")
    public void uncheckInTeam(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.uncheckInTeam(id, teamId);
    }

    @PatchMapping("{id}/teams/{teamId}/remove")
    public void removeTeamFromTournament(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.removeTeamFromTournament(teamId, id);
    }

    @PatchMapping("{id}/start")
    public void startTournament(@PathVariable Long id) {
        tournamentService.startTournament(id);
    }
}
