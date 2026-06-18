package com.heliozz10.debetter.dto.in;

import jakarta.validation.constraints.Positive;

import java.util.List;

public record NewsGetParams(
        String searchTitle,
        List<String> tags,
        @Positive Long authorId
) {}
