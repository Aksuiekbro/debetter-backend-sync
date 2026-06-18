package com.heliozz10.debetter.dto.user.in;

public record UserLoginDto (
    String username,
    String password,
    boolean rememberMe
) {}
