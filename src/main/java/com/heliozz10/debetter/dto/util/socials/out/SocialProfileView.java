package com.heliozz10.debetter.dto.util.socials.out;

import com.heliozz10.debetter.content.util.socials.SocialPlatform;
import lombok.Data;

import java.io.Serializable;

@Data
public class SocialProfileView implements Serializable {
    private SocialPlatform socialPlatform;
    private String handle;
}
