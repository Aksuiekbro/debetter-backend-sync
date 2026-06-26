package com.heliozz10.debetter.dto.tournament.team.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamUpdateOrganizerDto (
        @Size(min = 1, max = 50) String name,
        @Size(min = 1, max = 50) String club,
        @Size(max = 3) List<@Valid ParticipantSelectorDto> members
) {}
