package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.dto.tournament.in.TournamentMapFormDto;
import com.heliozz10.debetter.dto.tournament.out.TournamentMapView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapMapper;
import com.heliozz10.debetter.service.tournament.TournamentMapService;
import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/map")
public class TournamentMapController {
    private final TournamentMapService tournamentMapService;
    private final TournamentMapMapper tournamentMapMapper;

    @GetMapping
    public TournamentMapView getMap(@PathVariable Long tournamentId) {
        return tournamentMapMapper.toTournamentMapView(tournamentMapService.getMap(tournamentId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PostMapping
    public TournamentMapView createMap(
            @PathVariable Long tournamentId,
            @Validated({OnCreate.class, Default.class}) @RequestPart("data") TournamentMapFormDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return tournamentMapMapper.toTournamentMapView(tournamentMapService.createMap(dto, image, tournamentId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping
    public TournamentMapView updateMap(
            @PathVariable Long tournamentId,
            @Valid @RequestPart("data") TournamentMapFormDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return tournamentMapMapper.toTournamentMapView(tournamentMapService.updateMap(dto, image, tournamentId));
    }
}
