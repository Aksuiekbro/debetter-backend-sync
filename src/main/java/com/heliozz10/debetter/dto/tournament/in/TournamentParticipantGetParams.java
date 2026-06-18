package com.heliozz10.debetter.dto.tournament.in;

import jakarta.validation.constraints.Size;

public record TournamentParticipantGetParams(
        @Size(min = 1, max = 20) String searchUsername,
        @Size(min = 1, max = 20) String searchFirstName,
        @Size(min = 1, max = 20) String searchLastName,
        @Size(min = 1, max = 50) String searchEmail,
        Integer minSpeakerScore,
        Integer maxSpeakerScore
) {}
