package com.heliozz10.debetter.dto.user.in;

import com.heliozz10.debetter.dto.user.profile.in.CityDto;
import com.heliozz10.debetter.dto.user.profile.in.InstitutionDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateDto(
        @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$",
                message = "Username must be alphanumeric and 3–20 characters long") String username,
        @Size(min = 8, max = 32) String oldPassword,
        @Size(min = 8, max = 32) String newPassword,
        @Email @Size(min = 1, max = 50) String email,
        @Size(min = 1, max = 50) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String firstName,
        @Size(min = 1, max = 50) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String lastName,
        @Valid CityDto city,
        @Valid InstitutionDto institution
) {
    public UserUpdateDto {
        username = normalize(username);
        email = normalize(email);
        firstName = normalize(firstName);
        lastName = normalize(lastName);
    }

    private static String normalize(String value) {
        return value == null ? null : value.strip();
    }
}
