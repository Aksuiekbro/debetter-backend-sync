package com.heliozz10.debetter.dto.tournament.match.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ParticipantScoreDto (
        @NotNull @Positive Long participantId,
        @NotNull @PositiveOrZero Integer score
) {}