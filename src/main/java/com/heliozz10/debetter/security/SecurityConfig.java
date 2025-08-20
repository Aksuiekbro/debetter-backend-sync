package com.heliozz10.debetter.security;

import com.heliozz10.debetter.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final AuthProvider authProvider;
    private final UserService userService;
    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authenticationProvider(authProvider)
                .formLogin(form -> form
                        .loginProcessingUrl("/auth/login")
                        .loginPage("/auth/login")
                )
                .rememberMe(rememberMe -> rememberMe
                        .userDetailsService(userService)
                        .key(environment.getProperty("security.remember-me.key"))
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 30))
                .userDetailsService(userService)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .build();
    }
}
