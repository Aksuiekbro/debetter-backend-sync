package com.heliozz10.debetter.dto.tournament.match.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MatchResultDto(
        @NotNull @Positive Long matchId,
        @Valid List<TeamResultDto> teamResults,
        @Valid List<ParticipantScoreDto> participantScores
) {}