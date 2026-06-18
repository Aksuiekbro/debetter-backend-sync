package com.heliozz10.debetter.dto.user.profile.in;

import jakarta.validation.constraints.Size;

public record CityGetParams (
        @Size(min = 1, max = 20) String searchName
) {}
