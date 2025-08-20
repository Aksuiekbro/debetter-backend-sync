package com.heliozz10.debetter.controller.tournament.round;

import com.heliozz10.debetter.dto.tournament.round.in.RoundUpdateDto;
import com.heliozz10.debetter.dto.tournament.round.out.RoundView;
import com.heliozz10.debetter.mapper.tournament.round.RoundMapper;
import com.heliozz10.debetter.service.tournament.round.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/round-groups/{roundGroupId}/rounds")
public class RoundController {
    private final RoundService roundService;
    private final RoundMapper roundMapper;

    @GetMapping
    public List<RoundView> getRoundsByRoundGroupId(@PathVariable Long roundGroupId) {
        return roundMapper.toRoundViews(roundService.getRoundsByRoundGroupId(roundGroupId));
    }

    @PatchMapping("/{roundId}")
    public void updateRound(@PathVariable Long tournamentId, @PathVariable Long roundId, @RequestBody RoundUpdateDto roundUpdateDto) {
        roundService.updateRound(roundUpdateDto, tournamentId, roundId);
    }

    @DeleteMapping("/{roundId}")
    public void deleteRound(@PathVariable Long tournamentId, @PathVariable Long roundId) {
        roundService.deleteRound(tournamentId, roundId);
    }
}
