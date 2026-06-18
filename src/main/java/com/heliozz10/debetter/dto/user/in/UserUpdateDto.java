package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.dto.user.profile.in.CityDto;
import com.heliozz10.debetter.dto.user.profile.in.InstitutionDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateDto(
        @Size(min = 3, max = 20) String username,
        @Size(min = 8, max = 32) String oldPassword,
        @Size(min = 8, max = 32) String newPassword,
        @Size(min = 1, max = 50) String email,
        @Size(min = 1, max = 20) String firstName,
        @Size(min = 1, max = 20) String lastName,
        @Valid CityDto city,
        @Valid InstitutionDto institution
) {
}
