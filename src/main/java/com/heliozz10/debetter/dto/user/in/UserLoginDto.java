package com.heliozz10.debetter.dto.user.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserLoginDto (
    @NotBlank String username,
    @NotNull @Size(min = 1) String password,
    boolean rememberMe
) {}
