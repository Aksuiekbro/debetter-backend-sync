package com.heliozz10.debetter.dto.util.socials.in;

import com.heliozz10.debetter.content.util.socials.SocialPlatform;

public record SocialProfileDto(
        SocialPlatform platform,
        String handle,
        Boolean isPublic
) {
}
