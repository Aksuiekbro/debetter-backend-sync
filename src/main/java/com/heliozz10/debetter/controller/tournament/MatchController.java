package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchUpdateDto;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import com.heliozz10.debetter.mapper.tournament.MatchMapper;
import com.heliozz10.debetter.service.tournament.MatchService;
import com.heliozz10.debetter.service.tournament.round.RoundService;
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
@RequestMapping("/tournaments/{tournamentId}/round-groups/{roundGroupId}/rounds/{roundId}/matches")
public class MatchController {
    private final MatchService matchService;
    private final MatchMapper matchMapper;
    private final RoundService roundService;

    @GetMapping
    public PageableResult<MatchView> getMatchesByRoundId(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId,
            Authentication authentication,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Match> matches = matchService.getVisibleMatchesByRoundId(tournamentId, roundGroupId, roundId, authentication, pageable);
        return new PageableResult<>(
                matchMapper.toMatchViews(matches.getContent()),
                matches.getTotalElements(),
                matches.getTotalPages()
        );
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{matchId}")
    public MatchView updateMatch(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId,
            @PathVariable Long matchId,
            @Valid @RequestBody MatchUpdateDto matchUpdateDto
    ) {
        return matchMapper.toMatchView(matchService.updateMatch(tournamentId, roundGroupId, roundId, matchId, matchUpdateDto));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/results")
    public void submitMatchResults(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId,
            @Valid @RequestBody List<MatchResultDto> matchResultDto
    ) {
        matchService.submitMatchResults(tournamentId, roundGroupId, roundId, matchResultDto);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/randomize")
    public void randomizeMatches(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId
    ) {
        roundService.regenerateMatches(tournamentId, roundGroupId, roundId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/publish")
    public void publishMatches(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId
    ) {
        roundService.publishMatches(tournamentId, roundGroupId, roundId);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping
    public void clearMatches(
            @PathVariable Long tournamentId,
            @PathVariable Long roundGroupId,
            @PathVariable Long roundId
    ) {
        roundService.clearMatches(tournamentId, roundGroupId, roundId);
    }
}
