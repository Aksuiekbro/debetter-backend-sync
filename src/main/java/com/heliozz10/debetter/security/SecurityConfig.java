package com.heliozz10.debetter.security;

import com.heliozz10.debetter.service.user.UserService;
import com.heliozz10.debetter.repository.user.UserRepository;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RememberMeServices rememberMeServices,
                                           AuthProvider authProvider, UserService userService,
                                           Environment environment) throws Exception {
        String servletPath = environment.getProperty("spring.mvc.servlet.path", "");
        var logoutRequest = PathPatternRequestMatcher.withDefaults()
                .basePath("/".equals(servletPath) ? "" : servletPath)
                .matcher(HttpMethod.POST, "/auth/logout");
        return http
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(httpRequests -> httpRequests
                        .requestMatchers("/auth/**", "/uploads/**", "/news/**", "/cities", "/institutions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tournaments", "/tournaments/**", "/api/tournaments", "/api/tournaments/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutRequestMatcher(logoutRequest)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200)))
                .rememberMe(rememberMe -> rememberMe
                        .key(environment.getRequiredProperty("security.remember-me.key"))
                        .rememberMeServices(rememberMeServices))
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
            Environment environment,
            UserRepository users,
            PlatformTransactionManager transactionManager
    ) {
        JsonRememberMeServices services =
                new JsonRememberMeServices(
                        environment.getRequiredProperty("security.remember-me.key"),
                        userDetailsService,
                        tokenRepository,
                        users,
                        transactionManager
                );

        services.setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        services.setAlwaysRemember(false); // only explicit JSON opt-in

        return services;
    }


    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);

        // The Liquibase migration owns persistent_logins; preserve existing tokens at startup.

        return repo;
    }
}
