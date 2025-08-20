package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.content.user.Role;

public record UserGetParams(
        String searchUsername,
        String searchFirstName,
        String searchLastName,
        String searchEmail,
        String searchSocialProfileHandle,
        Role role
) {}
