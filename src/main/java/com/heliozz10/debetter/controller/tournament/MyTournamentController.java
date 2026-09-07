package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.service.tournament.MyTournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/mine")
public class MyTournamentController {
    private final MyTournamentService myTournamentService;
    private final TournamentMapper tournamentMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageableResult<TournamentView> getMyTournaments(
            @Valid @ModelAttribute TournamentGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            Authentication authentication
    ) {
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Authenticated user principal is required");
        }

        Page<Tournament> tournaments = myTournamentService.getMyTournaments(user.getId(), params, pageable);
        return new PageableResult<>(
                tournamentMapper.toTournamentViews(tournaments.getContent()),
                tournaments.getTotalElements(),
                tournaments.getTotalPages()
        );
    }
}
