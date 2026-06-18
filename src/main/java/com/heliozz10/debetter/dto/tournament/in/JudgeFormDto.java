package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JudgeFormDto(
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 50) String fullName,
        @Pattern(
                regexp = "^\\+?[0-9]{10,15}$",
                message = "Phone number must be valid (10–15 digits, optional leading +)"
        ) String phoneNumber,
        @Size(min = 1, max = 50) String email,
        Boolean checkedIn
) {
}
