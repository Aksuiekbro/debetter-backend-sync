package com.heliozz10.debetter.dto.tournament.in;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackGetParams(
        String searchTitle,
        List<String> tags,
        Boolean edited,
        LocalDateTime timestampFrom,
        LocalDateTime timestampTo
) {}