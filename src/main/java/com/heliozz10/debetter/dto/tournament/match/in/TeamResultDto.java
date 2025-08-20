package com.heliozz10.debetter.dto.tournament.match.in;

import java.util.List;

public record TeamResultDto (
    Long teamId,
    List<ParticipantScoreDto> participantScores
) {}
