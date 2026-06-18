package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.dto.user.profile.in.CityDto;
import com.heliozz10.debetter.dto.user.profile.in.InstitutionDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegistrationDto (
        @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$",
                message = "Username must be alphanumeric and 3–20 characters long")
        @NotNull String username,
        @NotNull @Size(min = 8, max = 32) String password,
        @NotNull @Email @Size(min = 1, max = 50) String email,
        @NotNull @Size(min = 1, max = 50) String firstName,
        @NotNull @Size(min = 1, max = 50) String lastName,
        @NotNull Role role,
        @Valid CityDto city,
        @Valid InstitutionDto institution
) {
}
