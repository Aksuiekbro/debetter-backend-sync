package com.heliozz10.debetter.dto.in;

import java.util.List;

public record NewsGetParams(
        String searchTitle,
        List<String> tags,
        Long authorId
) {}
