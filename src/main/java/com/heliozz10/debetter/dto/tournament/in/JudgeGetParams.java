package com.heliozz10.debetter.dto.tournament.in;

import jakarta.validation.constraints.Size;

public record JudgeGetParams(
        @Size(min = 1, max = 50) String searchFullName,
        @Size(min = 1, max = 50) String searchEmail,
        @Size(min = 1, max = 50) String searchSocialProfileHandle,
        @Size(min = 1, max = 20) String phoneNumber,
        Boolean checkedIn
) {}