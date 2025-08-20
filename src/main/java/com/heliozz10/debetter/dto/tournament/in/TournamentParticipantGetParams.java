package com.heliozz10.debetter.dto.tournament.in;

public record TournamentParticipantGetParams(
        String searchUsername,
        String searchFirstName,
        String searchLastName,
        String searchEmail,
        Integer minSpeakerScore,
        Integer maxSpeakerScore
) {}
