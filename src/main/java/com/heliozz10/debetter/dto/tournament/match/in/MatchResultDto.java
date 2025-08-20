package com.heliozz10.debetter.dto.tournament.match.in;

import java.util.List;

public record MatchResultDto(
        Long matchId,
        List<TeamResultDto> teamResults,
        List<ParticipantScoreDto> participantScores
) {}