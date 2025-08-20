package com.heliozz10.debetter.dto.tournament.in;

public record JudgeFormDto(
        String fullName,
        String phoneNumber,
        String email,
        Long tournamentId
) {
}
