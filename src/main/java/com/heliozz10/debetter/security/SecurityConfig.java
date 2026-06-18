package com.heliozz10.debetter.security;

import com.heliozz10.debetter.service.user.UserService;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final AuthProvider authProvider;
    private final UserService userService;
    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RememberMeServices rememberMeServices) throws Exception {
        return http
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(httpRequests -> httpRequests
                        .requestMatchers("/auth/**", "/uploads/**", "/news/**", "/cities", "/institutions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tournaments", "/tournaments/**", "/api/tournaments", "/api/tournaments/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutUrl("/auth/logout"))
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices))
//                        .userDetailsService(userService)
//                        .key(environment.getProperty("security.remember-me.key"))
//                        .rememberMeParameter("remember-me")
//                        .tokenValiditySeconds(60 * 60 * 24 * 30))
                .userDetailsService(userService)
                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .csrfTokenRequestHandler(new CustomCsrfTokenRequestHandler())
                        .disable())
                .cors(cors -> {})
                .build();
    }

    @Bean
    public RememberMeServices rememberMeServices(
            UserDetailsService userDetailsService,
            PersistentTokenRepository tokenRepository,
            Environment environment
    ) {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(
                        environment.getProperty("security.remember-me.key"),
                        userDetailsService,
                        tokenRepository
                );

        services.setParameter("remember-me");
        services.setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        services.setAlwaysRemember(false); // only if checkbox checked

        return services;
    }


    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);

        // repo.setCreateTableOnStartup(true);

        return repo;
    }
}
