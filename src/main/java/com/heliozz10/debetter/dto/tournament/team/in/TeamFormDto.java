package com.heliozz10.debetter.dto.tournament.team.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamFormDto(
        @NotNull @Size(min = 1, max = 120) String name,
        @NotNull @Size(min = 1, max = 120) String club,
        @Positive Long creatorId,
        @Valid List<ParticipantSelectorDto> invitedParticipants
) {
}
