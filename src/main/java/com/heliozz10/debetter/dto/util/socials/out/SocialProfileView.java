package com.heliozz10.debetter.dto.util.socials.out;

import com.heliozz10.debetter.content.util.socials.SocialPlatform;
import lombok.Data;

@Data
public class SocialProfileView {
    private SocialPlatform socialPlatform;
    private String handle;
}
