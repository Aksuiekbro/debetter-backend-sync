package com.heliozz10.debetter.dto.tournament.announcement.in;

import jakarta.validation.constraints.Size;

public record CommentDto (
        @Size(min = 1, max = 200) String content
) {}
