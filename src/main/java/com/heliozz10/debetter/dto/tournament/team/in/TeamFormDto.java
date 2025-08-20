package com.heliozz10.debetter.dto.tournament.team.in;

import java.util.List;

public record TeamFormDto(
        String name,
        String club,
        Long creatorId,
        List<ParticipantSelectorDto> invitedParticipants
) {
}
