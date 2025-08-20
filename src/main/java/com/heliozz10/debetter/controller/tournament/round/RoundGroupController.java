package com.heliozz10.debetter.controller.tournament.round;

import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.dto.tournament.in.DebateFormatDto;
import com.heliozz10.debetter.dto.tournament.round.out.RoundGroupView;
import com.heliozz10.debetter.mapper.tournament.round.RoundGroupMapper;
import com.heliozz10.debetter.service.tournament.TournamentService;
import com.heliozz10.debetter.service.tournament.round.RoundGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/round-groups")
public class RoundGroupController {
    private final RoundGroupService roundGroupService;
    private final RoundGroupMapper roundGroupMapper;

    @GetMapping
    public List<RoundGroupView> getRoundGroupsByTournamentId(@PathVariable Long tournamentId) {
        return roundGroupMapper.toRoundGroupViews(roundGroupService.getRoundGroupsByTournamentId(tournamentId));
    }

    @PatchMapping
    public void changeRoundGroupFormat(@PathVariable Long tournamentId, @RequestParam RoundGroupType roundGroupType, @RequestBody DebateFormatDto format) {
        roundGroupService.changeRoundGroupFormat(roundGroupType, format.format(), tournamentId);
    }

    @PatchMapping("/{roundGroupId}/proceed")
    public void proceedToNextRound(@PathVariable Long tournamentId, @PathVariable Long roundGroupId) {
        roundGroupService.proceedToNextRound(tournamentId, roundGroupId);
    }
}
