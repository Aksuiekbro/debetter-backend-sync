package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.content.user.Role;
import jakarta.validation.constraints.Size;

public record UserGetParams(
        @Size(min = 1, max = 20) String searchUsername,
        @Size(min = 1, max = 20) String searchFirstName,
        @Size(min = 1, max = 20) String searchLastName,
        @Size(min = 1, max = 50) String searchEmail,
        @Size(min = 1, max = 50) String searchSocialProfileHandle,
        Role role
) {}
