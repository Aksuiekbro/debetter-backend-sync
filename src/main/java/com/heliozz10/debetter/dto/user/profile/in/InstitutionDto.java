package com.heliozz10.debetter.dto.user.profile.in;

import jakarta.validation.constraints.Size;

public record InstitutionDto(
    @Size(min = 1, max = 100) String name
) {
}
