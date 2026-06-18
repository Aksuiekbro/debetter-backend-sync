package com.heliozz10.debetter.controller.tournament.round;

import com.heliozz10.debetter.dto.tournament.round.in.RoundUpdateDto;
import com.heliozz10.debetter.dto.tournament.round.out.RoundView;
import com.heliozz10.debetter.dto.tournament.round.out.SimpleRoundView;
import com.heliozz10.debetter.mapper.tournament.round.RoundMapper;
import com.heliozz10.debetter.service.tournament.round.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/round-groups/{roundGroupId}/rounds")
public class RoundController {
    private final RoundService roundService;
    private final RoundMapper roundMapper;

    @GetMapping
    public List<SimpleRoundView> getRoundsByRoundGroupId(@PathVariable Long tournamentId, @PathVariable Long roundGroupId) {
        return roundMapper.toSimpleRoundViews(roundService.getRoundsByTournamentIdAndRoundGroupId(tournamentId, roundGroupId));
    }

    @GetMapping("/{id}")
    public RoundView getRoundById(@PathVariable Long tournamentId, @PathVariable Long roundGroupId, @PathVariable Long id) {
        return roundMapper.toRoundView(roundService.getRoundByTournamentIdAndRoundGroupIdAndId(tournamentId, roundGroupId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{id}")
    public void updateRound(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody RoundUpdateDto roundUpdateDto) {
        roundService.updateRound(roundUpdateDto, tournamentId, id);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping("/{id}")
    public void deleteRound(@PathVariable Long tournamentId, @PathVariable Long id) {
        roundService.deleteRound(tournamentId, id);
    }
}
