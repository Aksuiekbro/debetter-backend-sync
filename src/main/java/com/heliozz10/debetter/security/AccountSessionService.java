package com.heliozz10.debetter.security;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.Authority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;


/** Replace the session snapshot only after a committed self-edit, preserving its trust level. */
@Service
@RequiredArgsConstructor
public class AccountSessionService {
    private final RememberMeServices rememberMeServices;
    private final Environment environment;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public void refreshSelf(User updated, Authentication authentication,
                            HttpServletRequest request, HttpServletResponse response) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User previous)
                || !previous.getId().equals(updated.getId())) {
            return;
        }
        // Do not mutate the existing session-held entity or share the managed entity with it.
        User principal = new User(updated.getId(), updated.getUsername(), updated.getPassword(),
                updated.getFirstName(), updated.getLastName(), updated.getEmail(), updated.getImageUrl(),
                previous.getAuthorities().stream().map(Authority.class::cast).toList(), updated.getRole(), updated.getProfile(),
                updated.getSocialProfiles(), updated.getCreatedAt(), updated.getUsernameLastEditedAt());
        AbstractAuthenticationToken refreshed;
        if (authentication instanceof RememberMeAuthenticationToken) {
            refreshed = new RememberMeAuthenticationToken(environment.getRequiredProperty("security.remember-me.key"),
                    principal, authentication.getAuthorities());
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            refreshed = UsernamePasswordAuthenticationToken.authenticated(principal, null, authentication.getAuthorities());
        } else {
            // Do not promote an unknown authentication mechanism to password authentication.
            return;
        }
        refreshed.setDetails(authentication.getDetails());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(refreshed);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        if (!previous.getUsername().equals(updated.getUsername())) {
            // Old tokens were revoked transactionally. Keep this session and clear its stale cookie.
            rememberMeServices.loginFail(request, response);
        }
    }
}
