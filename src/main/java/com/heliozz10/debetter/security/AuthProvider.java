package com.heliozz10.debetter.security;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AuthProvider implements AuthenticationProvider {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.getPrincipal() == null || authentication.getCredentials() == null) {
            throw new BadCredentialsException("Invalid username or password");
        }
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        User user;
        try {
            user = userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}