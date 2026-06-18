package com.heliozz10.debetter.dto.tournament.in;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackGetParams(
        @Size(min = 1, max = 50) String searchTitle,
        List<String> tags,
        Boolean edited,
        @PastOrPresent LocalDateTime timestampFrom,
        @PastOrPresent LocalDateTime timestampTo
) {}