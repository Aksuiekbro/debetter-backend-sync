package com.heliozz10.debetter.service.user.profile;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.Profile;

public interface ProfileService {
    Profile createProfile(Long userId);
}
