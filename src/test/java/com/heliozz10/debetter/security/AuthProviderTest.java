package com.heliozz10.debetter.security;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthProviderTest {
    private final UserService users = mock(UserService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AuthProvider provider = new AuthProvider(users, encoder);

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("accountuser");
        user.setPassword(encoder.encode("exact password "));
        user.setAuthorities(List.of());
        when(users.loadUserByUsername("accountuser")).thenReturn(user);
        when(users.loadUserByUsername("unknown")).thenThrow(new UsernameNotFoundException("User not found"));
    }

    @Test
    void wrongPasswordAndUnknownUsernameHaveTheSameSafeFailure() {
        var wrong = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(token("accountuser", "wrong-password")));
        var unknown = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(token("unknown", "wrong-password")));
        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @Test
    void absentCredentialsFailDeliberately() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(token("accountuser", null)));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(token(null, "password")));
    }

    @Test
    void successPreservesExactPasswordComparisonAndDoesNotRetainCredentials() {
        Authentication result = provider.authenticate(token("accountuser", "exact password "));
        assertTrue(result.isAuthenticated());
        assertEquals("accountuser", result.getName());
        assertNull(result.getCredentials());
        assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(token("accountuser", "exact password")));
    }

    private Authentication token(String username, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(username, password);
    }
}
