package com.heliozz10.debetter.security;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Honor opt-in from the authenticated JSON endpoint, never a query parameter. */
public class JsonRememberMeServices extends PersistentTokenBasedRememberMeServices {
    public static final String JSON_OPT_IN_ATTRIBUTE = JsonRememberMeServices.class.getName() + ".optIn";
    private final PersistentTokenRepository tokens;
    private final UserRepository users;
    private final TransactionTemplate transactions;

    public JsonRememberMeServices(String key, UserDetailsService userDetails, PersistentTokenRepository tokens,
                                  UserRepository users, PlatformTransactionManager transactionManager) {
        super(key, userDetails, tokens);
        this.tokens = tokens;
        this.users = users;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
        return Boolean.TRUE.equals(request.getAttribute(JSON_OPT_IN_ATTRIBUTE));
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String authenticatedName = authentication.getName();
        transactions.executeWithoutResult(status -> {
            if (!(authentication.getPrincipal() instanceof User principal)
                    || users.findForUpdateById(principal.getId()).isEmpty()
                    // The locking query may return an already-managed, stale login principal.
                    // Read a scalar from the database after the lock instead of trusting that entity.
                    || users.findCurrentUsernameById(principal.getId()).filter(authenticatedName::equals).isEmpty()) {
                cancelCookie(request, response);
                return;
            }
            super.onLoginSuccess(request, response, authentication);
        });
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        if (extractRememberMeCookie(request) == null) {
            return super.autoLogin(request, response);
        }
        AutoLoginResult result = transactions.execute(status -> {
            try {
                return new AutoLoginResult(super.autoLogin(request, response), null);
            } catch (CookieTheftException exception) {
                // Spring has already removed the mismatched tokens. Commit that cleanup
                // before propagating authentication failure; database exceptions still roll back.
                return new AutoLoginResult(null, exception);
            }
        });
        if (result.theft() != null) {
            throw result.theft();
        }
        return result.authentication();
    }

    private record AutoLoginResult(Authentication authentication, CookieTheftException theft) {}

    @Override
    protected UserDetails processAutoLoginCookie(String[] parts, HttpServletRequest request, HttpServletResponse response) {
        if (parts.length == 2) {
            PersistentRememberMeToken beforeLock = tokens.getTokenForSeries(parts[0]);
            if (beforeLock == null || users.findForUpdateByUsername(beforeLock.getUsername()).isEmpty()) {
                throw new RememberMeAuthenticationException("Persistent login is no longer valid");
            }
            // The superclass rereads the token after this lock and rejects expired cookies.
            // Ordinary expiry must preserve other devices' still-valid tokens.
        }
        return super.processAutoLoginCookie(parts, request, response);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            transactions.executeWithoutResult(status -> {
                if (authentication != null && authentication.getPrincipal() instanceof User principal
                        && principal.getId() != null) {
                    // Another session may have renamed this account since this principal was saved.
                    if (users.findForUpdateById(principal.getId()).isPresent()) {
                        users.findCurrentUsernameById(principal.getId()).ifPresent(tokens::removeUserTokens);
                    }
                } else {
                    revokeCookieTokens(request);
                }
            });
        } finally {
            cancelCookie(request, response);
        }
    }

    private void revokeCookieTokens(HttpServletRequest request) {
        // LogoutFilter runs before cookie authentication, so no principal may be available.
        String cookie = extractRememberMeCookie(request);
        if (cookie == null || cookie.isEmpty()) {
            return;
        }
        try {
            String[] parts = decodeCookie(cookie);
            if (parts.length != 2) {
                return;
            }
            PersistentRememberMeToken beforeLock = tokens.getTokenForSeries(parts[0]);
            if (beforeLock == null || users.findForUpdateByUsername(beforeLock.getUsername()).isEmpty()) {
                return;
            }
            PersistentRememberMeToken token = tokens.getTokenForSeries(parts[0]);
            if (token != null && MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8),
                    token.getTokenValue().getBytes(StandardCharsets.UTF_8))) {
                tokens.removeUserTokens(token.getUsername());
            }
        } catch (InvalidCookieException ignored) {
            // Invalid cookies never select a user for revocation.
        }
    }
}
