package com.heliozz10.debetter.dto.tournament.match.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MatchLocationDto(
        @NotNull @Positive Long matchId,
        @Size(max = 255) String location
) {
}
