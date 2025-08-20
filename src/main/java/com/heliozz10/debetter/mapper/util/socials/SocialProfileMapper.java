package com.heliozz10.debetter.mapper.util.socials;

import com.heliozz10.debetter.content.util.socials.SocialProfile;
import com.heliozz10.debetter.dto.util.socials.out.SocialProfileView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialProfileMapper {
    SocialProfileView toSocialProfileView(SocialProfile socialProfile);

    List<SocialProfileView> toSocialProfileViews(List<SocialProfile> socialProfiles);
}
