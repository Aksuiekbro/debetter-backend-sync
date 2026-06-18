package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackDto(
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 50) String title,
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 200) String content
) {}
