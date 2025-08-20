package com.heliozz10.debetter.dto.tournament.in;

public record JudgeGetParams(
        String searchFullName,
        String searchEmail,
        String searchSocialProfileHandle,
        String phoneNumber,
        Boolean checkedIn
) {}