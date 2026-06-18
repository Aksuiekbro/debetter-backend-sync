package com.heliozz10.debetter.dto.tournament.team.in;

import jakarta.validation.constraints.Size;

public record TeamUpdateOrganizerDto (
        @Size(min = 1, max = 50) String name
) {}
