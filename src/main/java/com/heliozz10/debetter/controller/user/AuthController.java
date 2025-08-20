package com.heliozz10.debetter.controller.user;

import com.heliozz10.debetter.dto.user.in.UserRegistrationDto;
import com.heliozz10.debetter.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody UserRegistrationDto user) {
        userService.createUser(user);
    }
}
