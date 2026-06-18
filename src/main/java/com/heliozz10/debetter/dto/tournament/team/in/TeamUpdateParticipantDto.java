package com.heliozz10.debetter.dto.tournament.team.in;

import jakarta.validation.constraints.Size;

public record TeamUpdateParticipantDto (
        @Size(min = 1, max = 50) String name,
        @Size(min = 1, max = 50) String club
) {}