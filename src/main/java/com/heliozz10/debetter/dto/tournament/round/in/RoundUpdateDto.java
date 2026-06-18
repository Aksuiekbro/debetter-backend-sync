package com.heliozz10.debetter.dto.tournament.round.in;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import jakarta.validation.constraints.Size;

public record RoundUpdateDto (
    @Size(min = 1, max = 20) String name,
    DebateFormat customFormat,
    Boolean matchesArePublic
) {}
