package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.TournamentParticipantGetParams;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.out.TournamentParticipantView;
import com.heliozz10.debetter.mapper.tournament.TournamentParticipantMapper;
import com.heliozz10.debetter.service.tournament.TournamentParticipantService;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/participants")
public class TournamentParticipantController {
    private final TournamentParticipantService tournamentParticipantService;
    private final TournamentParticipantMapper tournamentParticipantMapper;
    private final TournamentSecurity tournamentSecurity;

    @GetMapping
    @PreAuthorize("@tournamentSecurity.canReadTournament(authentication, #tournamentId)")
    public PageableResult<SimpleTournamentParticipantView> getTournamentParticipants(
            @PathVariable Long tournamentId,
            Authentication authentication,
            @Valid @ModelAttribute TournamentParticipantGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        boolean includeExactResults = tournamentSecurity.hasResultEntryPermission(authentication, tournamentId);
        if (!includeExactResults && (params.minSpeakerScore() != null || params.maxSpeakerScore() != null
                || pageable.getSort().stream().anyMatch(order -> order.getProperty().endsWith("Score")))) {
            throw new AccessDeniedException("Only tournament editors can filter or sort participants by exact scores");
        }
        Page<TournamentParticipant> participants = tournamentParticipantService.getParticipants(tournamentId, params, pageable);
        return new PageableResult<>(
                participants.getContent().stream().map(participant -> visibleParticipant(
                        tournamentParticipantService.toSimpleTournamentParticipantView(participant),
                        includeExactResults)).toList(),
                participants.getTotalElements(),
                participants.getTotalPages()
        );
    }

    @GetMapping("/{participantId}")
    @PreAuthorize("@tournamentSecurity.canReadTournament(authentication, #tournamentId)")
    public TournamentParticipantView getTournamentParticipant(@PathVariable Long tournamentId, @PathVariable Long participantId, Authentication authentication) {
        return visibleParticipant(tournamentParticipantService.toTournamentParticipantView(
                tournamentParticipantService.getParticipantByTournamentIdAndId(tournamentId, participantId)),
                tournamentSecurity.hasResultEntryPermission(authentication, tournamentId));
    }

    private <T extends SimpleTournamentParticipantView> T visibleParticipant(T view, boolean includeExactResults) {
        if (!includeExactResults) {
            view.setSpeakerScore(null);
        }
        return view;
    }
}
