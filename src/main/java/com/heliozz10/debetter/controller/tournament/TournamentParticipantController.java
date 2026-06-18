package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.TournamentParticipantGetParams;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.out.TournamentParticipantView;
import com.heliozz10.debetter.mapper.tournament.TournamentParticipantMapper;
import com.heliozz10.debetter.service.tournament.TournamentParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/participants")
public class TournamentParticipantController {
    private final TournamentParticipantService tournamentParticipantService;
    private final TournamentParticipantMapper tournamentParticipantMapper;

    @GetMapping
    public PageableResult<SimpleTournamentParticipantView> getTournamentParticipants(
            @PathVariable Long tournamentId,
            @Valid @ModelAttribute TournamentParticipantGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<TournamentParticipant> participants = tournamentParticipantService.getParticipants(tournamentId, params, pageable);
        return new PageableResult<>(
                participants.getContent().stream().map(tournamentParticipantService::toSimpleTournamentParticipantView).toList(),
                participants.getTotalElements(),
                participants.getTotalPages()
        );
    }

    @GetMapping("/{participantId}")
    public TournamentParticipantView getTournamentParticipant(@PathVariable Long tournamentId, @PathVariable Long participantId) {
        return tournamentParticipantService.toTournamentParticipantView(tournamentParticipantService.getParticipantByTournamentIdAndId(tournamentId, participantId));
    }
}
