package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.dto.user.profile.in.CityDto;
import com.heliozz10.debetter.dto.user.profile.in.InstitutionDto;

public record UserUpdateDto(
        String username,
        String oldPassword,
        String newPassword,
        String email,
        String firstName,
        String lastName,
        CityDto city,
        InstitutionDto institution
) {
}
