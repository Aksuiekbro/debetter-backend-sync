package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Schedule;
import com.heliozz10.debetter.dto.tournament.in.ScheduleFormDto;
import com.heliozz10.debetter.dto.tournament.out.ScheduleView;
import com.heliozz10.debetter.mapper.tournament.ScheduleMapper;
import com.heliozz10.debetter.service.tournament.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;
    private final ScheduleMapper scheduleMapper;

    @GetMapping
    public List<ScheduleView> getSchedulesByTournamentId(@PathVariable Long tournamentId) {
        return scheduleMapper.toScheduleViews(scheduleService.getSchedulesByTournamentId(tournamentId));
    }

    @GetMapping("/{id}")
    public ScheduleView getScheduleByTournamentIdAndId(@PathVariable Long tournamentId, @PathVariable Long id) {
        return scheduleMapper.toScheduleView(scheduleService.getScheduleByTournamentIdAndId(tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PostMapping
    public ScheduleView addScheduleToTournament(@PathVariable Long tournamentId, @Valid @RequestPart("data") ScheduleFormDto scheduleFormDto, @RequestPart(value = "image", required = false) MultipartFile image) {
        return scheduleMapper.toScheduleView(scheduleService.addScheduleToTournament(scheduleFormDto, image, tournamentId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping("/{id}")
    public void removeScheduleFromTournament(@PathVariable Long id, @PathVariable Long tournamentId) {
        scheduleService.removeScheduleFromTournament(id, tournamentId);
    }
}
