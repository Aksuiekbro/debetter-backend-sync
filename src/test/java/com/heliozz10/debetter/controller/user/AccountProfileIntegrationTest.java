package com.heliozz10.debetter.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.user.Authority;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.repository.user.AuthorityRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.security.AuthProvider;
import com.heliozz10.debetter.security.JsonRememberMeServices;
import com.heliozz10.debetter.service.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// No surrounding test transaction: each HTTP mutation must really commit or roll back.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account_profile;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/account-profile",
        "app.file-upload.storage-path=target/test-uploads/account-profile"
})
@AutoConfigureMockMvc
@Import(AccountProfileIntegrationTest.CacheTestConfiguration.class)
class AccountProfileIntegrationTest {
    private static final String PASSWORD = "TestPassword123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired AuthorityRepository authorities;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService userService;
    @Autowired AuthProvider authProvider;
    @Autowired RememberMeServices rememberMe;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    private User owner;

    @BeforeEach
    void setUp() {
        new ResourceDatabasePopulator(new ClassPathResource("db/changelog/create_persistent_logins.sql"))
                .execute(dataSource);
        owner = createUser(uniqueName());
    }

    @Test
    void selfEditPersistsRefreshesCacheAndSessionAndSupportsNewNameLogin() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        me(session).andExpect(jsonPath("$.username").value(owner.getUsername())); // prime cache
        String newName = uniqueName();
        patchUser(owner, session, Map.of("username", "  " + newName + "  ",
                "email", " changed" + newName + "@example.test ", "firstName", "  Updated  ",
                "lastName", "X".repeat(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newName))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.oldPassword").doesNotExist());
        User saved = users.findById(owner.getId()).orElseThrow();
        assertEquals(newName, saved.getUsername());
        assertEquals("changed" + newName + "@example.test", saved.getEmail());
        assertEquals(50, saved.getLastName().length());
        Authentication auth = authentication(session);
        assertEquals(owner.getId(), ((User) auth.getPrincipal()).getId());
        assertEquals(newName, auth.getName());
        assertNull(auth.getCredentials());
        me(session).andExpect(jsonPath("$.username").value(newName))
                .andExpect(jsonPath("$.firstName").value("Updated"));
        mvc.perform(get("/api/users/{id}", owner.getId()).servletPath("/api").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(newName));
        login(owner.getUsername(), PASSWORD, false).andExpect(status().isUnauthorized());
        login(newName, PASSWORD, false).andExpect(status().isOk());
    }

    @Test
    void participantAccountEditPreservesItsProfileAndLocationRelationships() throws Exception {
        String name = uniqueName();
        MockHttpSession session = session(mvc.perform(post("/api/auth/register").servletPath("/api")
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "username", name, "password", PASSWORD, "email", name + "@example.test",
                        "firstName", "Participant", "lastName", "Name", "role", "PARTICIPANT",
                        "city", Map.of("name", "Astana"), "institution", Map.of("name", "Test School"))))));
        User user = users.findByUsername(name).orElseThrow();
        ParticipantProfile profile = (ParticipantProfile) users.findById(user.getId()).orElseThrow().getProfile();
        Long profileId = profile.getId();
        Long cityId = profile.getCity().getId();
        Long institutionId = profile.getInstitution().getId();
        patchUser(user, session, Map.of("username", uniqueName(), "firstName", "UpdatedParticipant"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileId").value(profileId));
        ParticipantProfile reloaded = (ParticipantProfile) users.findById(user.getId()).orElseThrow().getProfile();
        assertEquals(profileId, reloaded.getId());
        assertEquals(cityId, reloaded.getCity().getId());
        assertEquals(institutionId, reloaded.getInstitution().getId());
    }

    @Test
    void nullAndOmittedPatchValuesPreserveStoredFieldsAndUnchangedValuesDoNotConflict() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        mvc.perform(patch("/api/users/{id}", owner.getId()).servletPath("/api").session(session)
                        .contentType(APPLICATION_JSON).content("{\"username\":null,\"email\":null,\"firstName\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(owner.getUsername()))
                .andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.firstName").value(owner.getFirstName()));
        patchUser(owner, session, Map.of("username", owner.getUsername(), "email", owner.getEmail()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"username\":\"   \"}", "{\"username\":\"x_1\"}", "{\"username\":\"ab\"}",
            "{\"username\":\"abcdefghijklmnopqrstu\"}", "{\"email\":\"not-an-email\"}", "{\"email\":\"  \"}",
            "{\"firstName\":\" \\t \"}", "{\"lastName\":\" \\n \"}"})
    void invalidAccountFieldsReturn400WithoutMutation(String body) throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        mvc.perform(patch("/api/users/{id}", owner.getId()).servletPath("/api").session(session)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(owner.getUsername(), users.findById(owner.getId()).orElseThrow().getUsername());
    }

    @Test
    void accountFieldLengthBoundariesMatchRegistration() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        patchUser(owner, session, Map.of("firstName", "F".repeat(21), "lastName", "L".repeat(50)))
                .andExpect(status().isOk());
        for (String field : List.of("firstName", "lastName")) {
            patchUser(owner, session, Map.of(field, "N".repeat(51))).andExpect(status().isBadRequest());
        }
        patchUser(owner, session, Map.of("email", uniqueName() + "@intranet")).andExpect(status().isOk());
        patchUser(owner, session, Map.of("email", "e".repeat(45) + "@a.test"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameOrEmailIs409WithoutPartialProfilePasswordOrTokenChanges() throws Exception {
        User other = createUser(uniqueName());
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, true));
        for (Map<String, String> conflict : List.of(
                Map.of("username", other.getUsername(), "firstName", "ShouldNotSave",
                        "oldPassword", PASSWORD, "newPassword", "DifferentPassword!"),
                Map.of("email", other.getEmail(), "username", uniqueName(), "firstName", "ShouldNotSave"))) {
            patchUser(owner, session, conflict).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
            User saved = users.findById(owner.getId()).orElseThrow();
            assertEquals(owner.getUsername(), saved.getUsername());
            assertEquals(owner.getFirstName(), saved.getFirstName());
            assertTrue(encoder.matches(PASSWORD, saved.getPassword()));
            assertEquals(1, tokenCount(owner.getUsername()));
            assertEquals(owner.getUsername(), authentication(session).getName());
        }
    }

    @Test
    void passwordChangeRequiresExactOldPasswordAndFailedMixedPatchIsAtomic() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, true));
        patchUser(owner, session, Map.of("username", uniqueName(), "firstName", "ShouldNotSave",
                "oldPassword", "IncorrectPassword", "newPassword", " new password "))
                .andExpect(status().isBadRequest());
        User saved = users.findById(owner.getId()).orElseThrow();
        assertEquals(owner.getUsername(), saved.getUsername());
        assertEquals(owner.getFirstName(), saved.getFirstName());
        assertEquals(1, tokenCount(owner.getUsername()));
        patchUser(owner, session, Map.of("newPassword", "newPassword123")).andExpect(status().isBadRequest());
        patchUser(owner, session, Map.of("oldPassword", PASSWORD)).andExpect(status().isBadRequest());
        patchUser(owner, session, Map.of("oldPassword", PASSWORD, "newPassword", " new password "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        assertTrue(encoder.matches(" new password ", users.findById(owner.getId()).orElseThrow().getPassword()));
        login(owner.getUsername(), "new password", false).andExpect(status().isUnauthorized());
        login(owner.getUsername(), " new password ", false).andExpect(status().isOk());
        login(owner.getUsername(), PASSWORD, false).andExpect(status().isUnauthorized());
    }

    @Test
    void whitespacePasswordsAreSuppliedValuesAndAreComparedExactly() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        patchUser(owner, session, Map.of("oldPassword", "        ", "newPassword", "         "))
                .andExpect(status().isBadRequest());
        patchUser(owner, session, Map.of("oldPassword", PASSWORD, "newPassword", "        "))
                .andExpect(status().isOk());
        login(owner.getUsername(), "        ", false).andExpect(status().isOk());
        patchUser(owner, session, Map.of("oldPassword", "        ", "newPassword", "         "))
                .andExpect(status().isOk());
        login(owner.getUsername(), "        ", false).andExpect(status().isUnauthorized());
        login(owner.getUsername(), "         ", false).andExpect(status().isOk());
    }

    @Test
    void databaseFailureAfterTokenRevocationRollsBackProfileAndTokensTogether() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, true));
        String forbiddenName = uniqueName();
        jdbc.execute("alter table _user add constraint account_test_failure check (username <> '" + forbiddenName + "')");
        try {
            patchUser(owner, session, Map.of("username", forbiddenName, "firstName", "ShouldNotSave",
                    "oldPassword", PASSWORD, "newPassword", "DifferentPassword!"))
                    .andExpect(status().isConflict());
            User saved = users.findById(owner.getId()).orElseThrow();
            assertEquals(owner.getUsername(), saved.getUsername());
            assertEquals(owner.getFirstName(), saved.getFirstName());
            assertTrue(encoder.matches(PASSWORD, saved.getPassword()));
            assertEquals(1, tokenCount(owner.getUsername()));
            assertEquals(owner.getUsername(), authentication(session).getName());
        } finally {
            jdbc.execute("alter table _user drop constraint account_test_failure");
        }
    }

    @Test
    void tokenRepositoryFailureCannotCommitRenameOrPasswordChanges() throws Exception {
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, true));
        // Simulate unavailable token storage using the real JDBC repository, without mocks.
        jdbc.execute("alter table persistent_logins rename to account_test_unavailable_tokens");
        try {
            assertThrows(jakarta.servlet.ServletException.class, () -> patchUser(owner, session, Map.of(
                    "username", uniqueName(), "firstName", "ShouldNotSave",
                    "oldPassword", PASSWORD, "newPassword", "DifferentPassword!")));
            User saved = users.findById(owner.getId()).orElseThrow();
            assertEquals(owner.getUsername(), saved.getUsername());
            assertEquals(owner.getFirstName(), saved.getFirstName());
            assertTrue(encoder.matches(PASSWORD, saved.getPassword()));
            assertEquals(owner.getUsername(), authentication(session).getName());
        } finally {
            jdbc.execute("alter table account_test_unavailable_tokens rename to persistent_logins");
        }
        assertEquals(1, tokenCount(owner.getUsername()));
    }

    @Test
    void otherUserAndAnonymousCannotEditAndAdminDoesNotAdoptEditedIdentity() throws Exception {
        User other = createUser(uniqueName());
        MockHttpSession session = session(login(owner.getUsername(), PASSWORD, false));
        patchUser(other, session, Map.of("firstName", "Unauthorized")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/{id}", other.getId()).servletPath("/api")
                        .contentType(APPLICATION_JSON).content("{\"firstName\":\"Unauthorized\"}"))
                .andExpect(status().is4xxClientError());
        Authority admin = authorities.findAll().stream().filter(a -> a.getName().equals("ADMIN")).findFirst()
                .orElseGet(() -> authorities.saveAndFlush(new Authority("ADMIN")));
        owner.setAuthorities(List.of(admin));
        users.saveAndFlush(owner);
        MockHttpSession adminSession = session(login(owner.getUsername(), PASSWORD, true));
        String newName = uniqueName();
        patchUser(other, adminSession, Map.of("username", newName)).andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("remember-me"));
        assertEquals(owner.getId(), ((User) authentication(adminSession).getPrincipal()).getId());
        assertEquals(owner.getUsername(), authentication(adminSession).getName());
        assertEquals("ADMIN", authentication(adminSession).getAuthorities().iterator().next().getAuthority());
        assertEquals(1, tokenCount(owner.getUsername()));
        me(adminSession).andExpect(jsonPath("$.id").value(owner.getId()));
    }

    @Test
    void wrongPasswordAndUnknownUserReturnIdentical401WithoutSessionOrPersistentTokens() throws Exception {
        MvcResult wrong = login(owner.getUsername(), "WrongPassword!", true)
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknown = login(uniqueName(), "WrongPassword!", true)
                .andExpect(status().isUnauthorized()).andReturn();
        assertEquals(wrong.getResponse().getContentAsString(), unknown.getResponse().getContentAsString());
        assertNull(wrong.getRequest().getSession(false));
        assertNull(unknown.getRequest().getSession(false));
        assertNull(wrong.getResponse().getCookie("remember-me"));
        assertNull(unknown.getResponse().getCookie("remember-me"));
        assertEquals(0, tokenCount(owner.getUsername()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"username\":null,\"password\":null}", "{\"username\":\"user\"}",
            "{\"password\":\"password\"}", "{\"username\":\"\",\"password\":\"\"}"})
    void missingCredentialsReturnDeliberate400(String body) throws Exception {
        mvc.perform(post("/api/auth/login").servletPath("/api").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void validLoginAndRegistrationPersistAuthenticatedSessionsWithoutPlaintextCredentials() throws Exception {
        MockHttpSession loginSession = session(login(owner.getUsername(), PASSWORD, false));
        assertNull(authentication(loginSession).getCredentials());
        me(loginSession).andExpect(jsonPath("$.id").value(owner.getId()));
        String name = uniqueName();
        MockHttpSession registered = session(mvc.perform(post("/api/auth/register").servletPath("/api")
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "username", name, "password", PASSWORD, "email", name + "@example.test",
                        "firstName", "Registered", "lastName", "Organizer", "role", "ORGANIZER")))));
        assertNull(authentication(registered).getCredentials());
        me(registered).andExpect(jsonPath("$.username").value(name));
        assertTrue(encoder.matches(PASSWORD, users.findByUsername(name).orElseThrow().getPassword()));
    }

    @Test
    void explicitJsonRememberMeCreatesJdbcTokenAndAuthenticatesWithoutSession() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        assertEquals(30 * 24 * 60 * 60, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertEquals(1, tokenCount(owner.getUsername()));
        MvcResult restored = mvc.perform(get("/api/users/me").servletPath("/api").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId())).andReturn();
        assertInstanceOf(RememberMeAuthenticationToken.class, authentication((MockHttpSession) restored.getRequest().getSession(false)));
    }

    @Test
    void omittedOrFalseJsonRememberMeCannotBeOverriddenByQueryParameter() throws Exception {
        login(owner.getUsername(), PASSWORD, false).andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("remember-me"));
        for (String suffix : List.of("", ",\"rememberMe\":false")) {
            mvc.perform(post("/api/auth/login").servletPath("/api").param("remember-me", "true")
                            .contentType(APPLICATION_JSON).content("{\"username\":\"" + owner.getUsername()
                                    + "\",\"password\":\"" + PASSWORD + "\"" + suffix + "}"))
                    .andExpect(status().isOk()).andExpect(cookie().doesNotExist("remember-me"));
        }
        assertEquals(0, tokenCount(owner.getUsername()));
    }

    @Test
    void logoutRevokesPersistentTokenAndClearsCookie() throws Exception {
        MvcResult signedIn = login(owner.getUsername(), PASSWORD, true).andExpect(status().isOk()).andReturn();
        Cookie cookie = signedIn.getResponse().getCookie("remember-me");
        mvc.perform(post("/api/auth/logout").servletPath("/api")
                        .session((MockHttpSession) signedIn.getRequest().getSession(false)).cookie(cookie))
                .andExpect(cookie().maxAge("remember-me", 0));
        assertEquals(0, tokenCount(owner.getUsername()));
        assertCookieRejected(cookie);
    }

    @Test
    void cookieOnlyLogoutAlsoRevokesServerToken() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        mvc.perform(post("/api/auth/logout").servletPath("/api").cookie(cookie))
                .andExpect(cookie().maxAge("remember-me", 0));
        assertEquals(0, tokenCount(owner.getUsername()));
        assertCookieRejected(cookie);
    }

    @Test
    void forgedCookieCannotRevokeAnotherUsersTokensOnLogout() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        String decoded = new String(java.util.Base64.getDecoder().decode(cookie.getValue()), java.nio.charset.StandardCharsets.UTF_8);
        String forged = decoded.substring(0, decoded.indexOf(':') + 1) + "wrong-token";
        Cookie invalid = new Cookie("remember-me", java.util.Base64.getEncoder()
                .encodeToString(forged.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(post("/api/auth/logout").servletPath("/api").cookie(invalid))
                .andExpect(cookie().maxAge("remember-me", 0));
        assertEquals(1, tokenCount(owner.getUsername()));
        mvc.perform(get("/api/users/me").servletPath("/api").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void expiredPersistentCookieCannotAuthenticateAndIsCleared() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        jdbc.update("update persistent_logins set last_used = ? where username = ?",
                Timestamp.from(Instant.now().minusSeconds(31L * 24 * 60 * 60)), owner.getUsername());
        assertCookieRejected(cookie);
        assertEquals(1, tokenCount(owner.getUsername()));
    }

    @Test
    void oneExpiredCookiePreservesAnotherDevicesLiveRememberedLogin() throws Exception {
        Cookie expiredCookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        String expiredSeries = jdbc.queryForObject("select series from persistent_logins where username = ?",
                String.class, owner.getUsername());
        Cookie liveCookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        jdbc.update("update persistent_logins set last_used = ? where series = ?",
                Timestamp.from(Instant.now().minusSeconds(31L * 24 * 60 * 60)), expiredSeries);
        assertEquals(2, tokenCount(owner.getUsername()));

        assertCookieRejected(expiredCookie);

        assertEquals(2, tokenCount(owner.getUsername()));
        mvc.perform(get("/api/users/me").servletPath("/api").cookie(liveCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId()));
        assertEquals(2, tokenCount(owner.getUsername()));
    }

    @Test
    void renameRevokesOldTokensAndOldUsernameReuseCannotTakeOverNewAccount() throws Exception {
        MvcResult signedIn = login(owner.getUsername(), PASSWORD, true).andExpect(status().isOk()).andReturn();
        Cookie cookie = signedIn.getResponse().getCookie("remember-me");
        MockHttpSession session = (MockHttpSession) signedIn.getRequest().getSession(false);
        String newName = uniqueName();
        patchUser(owner, session, Map.of("username", newName))
                .andExpect(status().isOk()).andExpect(cookie().maxAge("remember-me", 0));
        assertEquals(0, tokenCount(owner.getUsername()));
        assertEquals(0, tokenCount(newName));
        me(session).andExpect(jsonPath("$.id").value(owner.getId())).andExpect(jsonPath("$.username").value(newName));
        User replacement = createUser(owner.getUsername());
        assertNotEquals(replacement.getId(), owner.getId());
        assertCookieRejected(cookie);
    }

    @Test
    void rememberMeAuthenticatedRenamePreservesTrustLevelAndCurrentSession() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        String newName = uniqueName();
        MvcResult result = mvc.perform(patch("/api/users/{id}", owner.getId()).servletPath("/api").cookie(cookie)
                        .contentType(APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", newName))))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertInstanceOf(RememberMeAuthenticationToken.class, authentication(session));
        assertEquals(newName, authentication(session).getName());
        assertEquals(owner.getId(), ((User) authentication(session).getPrincipal()).getId());
        assertEquals(0, tokenCount(owner.getUsername()));
        // Cookie authentication rotates before the controller; the final header must cancel that cookie.
        String lastCookie = result.getResponse().getHeaders("Set-Cookie").getLast();
        assertTrue(lastCookie.contains("remember-me="));
        assertTrue(lastCookie.contains("Max-Age=0"));
        me(session).andExpect(jsonPath("$.username").value(newName));
    }

    @Test
    void staleSessionLogoutRevokesOnlyItsStableAccountAfterOldNameReuse() throws Exception {
        MockHttpSession staleSession = session(login(owner.getUsername(), PASSWORD, true));
        String newName = uniqueName();
        userService.updateUser(rename(newName), owner.getId()); // rename from another session
        Cookie currentOwnerCookie = rememberCookie(login(newName, PASSWORD, true));
        User replacement = createUser(owner.getUsername());
        Cookie replacementCookie = rememberCookie(login(replacement.getUsername(), PASSWORD, true));
        mvc.perform(post("/api/auth/logout").servletPath("/api").session(staleSession))
                .andExpect(status().isOk()).andExpect(cookie().maxAge("remember-me", 0));
        assertEquals(0, tokenCount(newName));
        assertEquals(1, tokenCount(replacement.getUsername()));
        assertCookieRejected(currentOwnerCookie);
        mvc.perform(get("/api/users/me").servletPath("/api").cookie(replacementCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(replacement.getId()));
    }

    @Test
    void cookieOnlyLogoutWaitsForConcurrentRenameAndRereadsToken() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();
        runWhileRenaming(() -> ((org.springframework.security.web.authentication.logout.LogoutHandler) rememberMe).logout(request, response, null));
        assertEquals(0, tokenCount(owner.getUsername()));
        assertEquals(0, response.getCookie("remember-me").getMaxAge());
    }

    @Test
    void staleAuthenticatedPrincipalCannotIssueTokensAfterRenameAndNameReuse() {
        Authentication stale = authProvider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(owner.getUsername(), PASSWORD));
        userService.updateUser(rename(uniqueName()), owner.getId());
        createUser(owner.getUsername());
        MockHttpServletRequest request = optedInRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        rememberMe.loginSuccess(request, response, stale);
        assertEquals(0, tokenCount(owner.getUsername()));
        assertEquals(0, response.getCookie("remember-me").getMaxAge());
    }

    @Test
    void alreadyManagedStaleLoginPrincipalCannotBypassTheFreshDatabaseNameCheck() throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try (var executor = Executors.newSingleThreadExecutor()) {
            Authentication stale = authProvider.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(owner.getUsername(), PASSWORD));
            assertTrue(entityManager.contains(stale.getPrincipal()));
            executor.submit(() -> userService.updateUser(rename(uniqueName()), owner.getId())).get(5, TimeUnit.SECONDS);
            assertEquals(owner.getUsername(), stale.getName()); // still managed, but stale like a login under OSIV
            MockHttpServletResponse response = new MockHttpServletResponse();
            rememberMe.loginSuccess(optedInRequest(), response, stale);
            assertEquals(0, tokenCount(owner.getUsername()));
            assertEquals(0, response.getCookie("remember-me").getMaxAge());
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            entityManager.close();
        }
    }

    @Test
    void stolenCookieDetectionCommitsTokenRevocationBeforeAuthenticationFailure() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        String decoded = new String(java.util.Base64.getDecoder().decode(cookie.getValue()), java.nio.charset.StandardCharsets.UTF_8);
        String forged = decoded.substring(0, decoded.indexOf(':') + 1) + "wrong-token";
        Cookie invalid = new Cookie("remember-me", java.util.Base64.getEncoder()
                .encodeToString(forged.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(invalid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(CookieTheftException.class, () -> rememberMe.autoLogin(request, response));
        assertEquals(0, response.getCookie("remember-me").getMaxAge());
        assertEquals(0, tokenCount(owner.getUsername()));
    }

    @Test
    void tokenIssuanceWaitsForConcurrentRenameAndCannotRecreateOldUsernameTokens() throws Exception {
        Authentication stale = authProvider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(owner.getUsername(), PASSWORD));
        MockHttpServletResponse response = new MockHttpServletResponse();
        runWhileRenaming(() -> rememberMe.loginSuccess(optedInRequest(), response, stale));
        createUser(owner.getUsername());
        assertEquals(0, tokenCount(owner.getUsername()));
        assertEquals(0, response.getCookie("remember-me").getMaxAge());
    }

    @Test
    void cookieRestorationWaitsForConcurrentRenameAndRereadsRevokedToken() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();
        runWhileRenaming(() -> assertNull(rememberMe.autoLogin(request, response)));
        createUser(owner.getUsername());
        assertEquals(0, tokenCount(owner.getUsername()));
        assertEquals(0, response.getCookie("remember-me").getMaxAge());
    }

    private void runWhileRenaming(Runnable concurrentAuthentication) throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = new TransactionTemplate(transactionManager).execute(status -> {
                users.findForUpdateById(owner.getId()).orElseThrow();
                CountDownLatch started = new CountDownLatch(1);
                Future<?> attempt = executor.submit(() -> {
                    started.countDown();
                    concurrentAuthentication.run();
                });
                try {
                    assertTrue(started.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                // A real user-row lock must keep issuance/restoration pending until rename commits.
                assertThrows(TimeoutException.class, () -> attempt.get(200, TimeUnit.MILLISECONDS));
                userService.updateUser(rename(uniqueName()), owner.getId());
                return attempt;
            });
            assertNotNull(pending);
            pending.get(5, TimeUnit.SECONDS);
        }
    }

    private MockHttpServletRequest optedInRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JsonRememberMeServices.JSON_OPT_IN_ATTRIBUTE, Boolean.TRUE);
        return request;
    }

    private UserUpdateDto rename(String name) {
        return new UserUpdateDto(name, null, null, null, null, null, null, null);
    }

    @Test
    void tokenStorageMigrationIsIdempotentAndPreservesExistingTokens() throws Exception {
        Cookie cookie = rememberCookie(login(owner.getUsername(), PASSWORD, true));
        new ResourceDatabasePopulator(new ClassPathResource("db/changelog/create_persistent_logins.sql"))
                .execute(dataSource);
        assertEquals(1, tokenCount(owner.getUsername()));
        mvc.perform(get("/api/users/me").servletPath("/api").cookie(cookie)).andExpect(status().isOk());
    }

    private ResultActions login(String username, String password, boolean rememberMe) throws Exception {
        return mvc.perform(post("/api/auth/login").servletPath("/api").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password, "rememberMe", rememberMe))));
    }

    private MockHttpSession session(ResultActions result) throws Exception {
        return (MockHttpSession) result.andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private Cookie rememberCookie(ResultActions result) throws Exception {
        Cookie cookie = result.andExpect(status().isOk()).andReturn().getResponse().getCookie("remember-me");
        assertNotNull(cookie);
        assertTrue(cookie.getMaxAge() > 0);
        return cookie;
    }

    private ResultActions patchUser(User user, MockHttpSession session, Map<String, String> fields) throws Exception {
        return mvc.perform(patch("/api/users/{id}", user.getId()).servletPath("/api").session(session)
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(fields)));
    }

    private ResultActions me(MockHttpSession session) throws Exception {
        return mvc.perform(get("/api/users/me").servletPath("/api").session(session)).andExpect(status().isOk());
    }

    private Authentication authentication(MockHttpSession session) {
        assertNotNull(session);
        return ((SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .getAuthentication();
    }

    private void assertCookieRejected(Cookie cookie) throws Exception {
        mvc.perform(get("/api/users/me").servletPath("/api").cookie(cookie))
                .andExpect(status().is4xxClientError()).andExpect(cookie().maxAge("remember-me", 0));
    }

    private int tokenCount(String username) {
        return jdbc.queryForObject("select count(*) from persistent_logins where username = ?", Integer.class, username);
    }

    private User createUser(String username) {
        User user = new User(username, encoder.encode(PASSWORD), uniqueName() + "@example.test", "Original", "Name", Role.ORGANIZER);
        user.setAuthorities(new ArrayList<>());
        user.setSocialProfiles(new ArrayList<>());
        return users.saveAndFlush(user);
    }

    private String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean @Primary
        CacheManager accountTestCacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
