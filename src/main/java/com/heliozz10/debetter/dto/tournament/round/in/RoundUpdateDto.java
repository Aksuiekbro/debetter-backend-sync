package com.heliozz10.debetter.dto.tournament.round.in;

import com.heliozz10.debetter.content.tournament.DebateFormat;

public record RoundUpdateDto (
    String name,
    DebateFormat customFormat,
    Boolean matchesArePublic
) {}
