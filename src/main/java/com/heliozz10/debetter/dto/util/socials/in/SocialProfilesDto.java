package com.heliozz10.debetter.dto.util.socials.in;

import jakarta.validation.Valid;

import java.util.List;

public record SocialProfilesDto (
    @Valid List<SocialProfileDto> socialProfiles
) {}
