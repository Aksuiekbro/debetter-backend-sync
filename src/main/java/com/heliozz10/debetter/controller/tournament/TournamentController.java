package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.TournamentFormDto;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.service.tournament.TournamentService;
import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

//TODO: fix mappers
@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;
    private final TournamentMapper tournamentMapper;

    private final UserMapper userMapper;

    private final TeamRepository teamRepository;

    @GetMapping
    public PageableResult<TournamentView> getTournaments(
            @Valid @ModelAttribute TournamentGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Tournament> tournaments = tournamentService.getTournaments(params, pageable);
        return new PageableResult<>(
                tournamentMapper.toTournamentViews(tournaments.getContent()),
                tournaments.getTotalElements(),
                tournaments.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public TournamentView getTournamentById(@PathVariable Long id) {
        return tournamentMapper.toTournamentView(tournamentService.getTournamentById(id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER'")
    @PostMapping
    public TournamentView createTournament(@Validated({OnCreate.class, Default.class}) @RequestPart("data") TournamentFormDto dto, @RequestPart(value = "image", required = false) MultipartFile image, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrganizerProfile profile = (OrganizerProfile) user.getProfile();
        return tournamentMapper.toTournamentView(tournamentService.createTournament(dto, image, profile.getId()));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("/{id}")
    public TournamentView updateTournament(@PathVariable Long id, @Valid @RequestPart("data") TournamentFormDto dto, @RequestPart(value = "image", required = false) MultipartFile image) {
        return tournamentMapper.toTournamentView(tournamentService.updateTournament(dto, image, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasFullPermission(principal, #id)")
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

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasFullPermission(principal, #id)")
    @DeleteMapping("/{id}/organizers/{organizerId}")
    public void removeOrganizerFromTournament(@PathVariable Long id, @PathVariable Long organizerId) {
        tournamentService.removeOrganizerFromTournament(id, organizerId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasFullPermission(principal, #id)")
    @PatchMapping("/{id}/enable")
    public void enableTournament(@PathVariable Long id) {
        tournamentService.enableTournament(id);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasFullPermission(principal, #id)")
    @PatchMapping("/{id}/disable")
    public void disableTournament(@PathVariable Long id) {
        tournamentService.disableTournament(id);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("{id}/teams/{teamId}/check-in")
    public void checkInTeam(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.checkInTeam(id, teamId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("{id}/teams/{teamId}/uncheck-in")
    public void uncheckInTeam(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.uncheckInTeam(id, teamId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("{id}/teams/{teamId}/disqualify")
    public void disqualifyTeam(@PathVariable Long id, @PathVariable Long teamId) {
        teamRepository.setTeamDisqualifiedByTournamentIdAndId(id, teamId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("{id}/teams/{teamId}/requalify")
    public void requalifyTeam(@PathVariable Long id, @PathVariable Long teamId) {
        teamRepository.setTeamNotDisqualifiedByTournamentIdAndId(id, teamId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @DeleteMapping("{id}/teams/{teamId}")
    public void removeTeamFromTournament(@PathVariable Long id, @PathVariable Long teamId) {
        tournamentService.removeTeamFromTournament(teamId, id);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #id)")
    @PatchMapping("{id}/start")
    public void startTournament(@PathVariable Long id) {
        tournamentService.startTournament(id);
    }
}
