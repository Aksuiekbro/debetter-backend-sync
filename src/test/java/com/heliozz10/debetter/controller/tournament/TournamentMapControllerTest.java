package com.heliozz10.debetter.controller.tournament;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.TournamentMapRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tournament_map_controller_test;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/tournament-map-controller",
        "app.file-upload.storage-path=target/test-uploads/tournament-map-controller"
})
@AutoConfigureMockMvc
@Transactional
@Import(TournamentMapControllerTest.CacheTestConfiguration.class)
class TournamentMapControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TournamentMapRepository tournamentMapRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Test
    void editorCanCreateAMapAndAnonymousViewerCanReadItAfterward() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.EDIT);

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Venue map"))
                .andExpect(jsonPath("$.description").value("Rooms and entrances"))
                .andExpect(jsonPath("$.imageUrl.url", startsWith(
                        "/uploads/images/tournament-maps/" + tournament.getId() + "-"
                )));

        assertTrue(tournamentMapRepository.findByTournamentId(tournament.getId()).isPresent());
        mockMvc.perform(get("/api/tournaments/{id}/map", tournament.getId()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Venue map"))
                .andExpect(jsonPath("$.description").value("Rooms and entrances"))
                .andExpect(jsonPath("$.imageUrl.url", startsWith("/uploads/images/tournament-maps/")));
    }

    @Test
    void editorCanUpdateMetadataAndReplaceTheMapImage() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.FULL);
        String createResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String originalUrl = objectMapper.readTree(createResponse).path("imageUrl").path("url").asText();

        String updateResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Updated venue map", "Use the north entrance"))
                        .file(image("replacement.png", "replacement image"))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated venue map"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updated = objectMapper.readTree(updateResponse);
        assertNotEquals(originalUrl, updated.path("imageUrl").path("url").asText());
    }

    @Test
    void editorCanPartiallyUpdateMetadataWithoutReplacingTheMapImage() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.EDIT);
        String createResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String originalUrl = objectMapper.readTree(createResponse).path("imageUrl").path("url").asText();

        String updateResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("""
                                {"title":"Updated venue map"}
                                """))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated venue map"))
                .andExpect(jsonPath("$.description").value("Rooms and entrances"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(originalUrl, objectMapper.readTree(updateResponse).path("imageUrl").path("url").asText());
    }

    @Test
    void editorCanReplaceOnlyTheMapImage() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.FULL);
        String createResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String originalUrl = objectMapper.readTree(createResponse).path("imageUrl").path("url").asText();

        String updateResponse = mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("{}"))
                        .file(image("replacement.png", "replacement image"))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Venue map"))
                .andExpect(jsonPath("$.description").value("Rooms and entrances"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotEquals(originalUrl, objectMapper.readTree(updateResponse).path("imageUrl").path("url").asText());
    }

    @Test
    void organizerWithoutEditPermissionCannotCreateAMap() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken viewer = organizerWithRole(tournament, TournamentRole.VIEW);

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "image"))
                        .servletPath("/api")
                        .with(authentication(viewer)))
                .andExpect(status().isForbidden());

        assertFalse(tournamentMapRepository.existsByTournamentId(tournament.getId()));
    }

    @Test
    void organizerWithoutEditPermissionCannotUpdateAMap() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.EDIT);
        UsernamePasswordAuthenticationToken viewer = organizerWithRole(tournament, TournamentRole.VIEW);

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Unauthorized title", "Unauthorized description"))
                        .file(image("replacement.png", "unauthorized replacement"))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .servletPath("/api")
                        .with(authentication(viewer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tournaments/{id}/map", tournament.getId()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Venue map"))
                .andExpect(jsonPath("$.description").value("Rooms and entrances"));
    }

    @Test
    void creatingASecondMapReturnsAConflict() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.EDIT);

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(image("venue.png", "first image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData("Second map", "Should not be created"))
                        .file(image("second.png", "second image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tournament map already exists"));
    }

    @Test
    void missingAndInvalidImagesReturnUsefulBadRequestMessages() throws Exception {
        Tournament missingImageTournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken firstEditor = organizerWithRole(
                missingImageTournament,
                TournamentRole.EDIT
        );
        mockMvc.perform(multipart("/api/tournaments/{id}/map", missingImageTournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .servletPath("/api")
                        .with(authentication(firstEditor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Map image is required"));

        Tournament invalidImageTournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken secondEditor = organizerWithRole(
                invalidImageTournament,
                TournamentRole.EDIT
        );
        mockMvc.perform(multipart("/api/tournaments/{id}/map", invalidImageTournament.getId())
                        .file(mapData("Venue map", "Rooms and entrances"))
                        .file(new MockMultipartFile("image", "venue.gif", "image/gif", "image".getBytes()))
                        .servletPath("/api")
                        .with(authentication(secondEditor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid file extension")));
    }

    @Test
    void invalidMetadataAndMissingMapReturnUsefulErrors() throws Exception {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        UsernamePasswordAuthenticationToken editor = organizerWithRole(tournament, TournamentRole.EDIT);

        mockMvc.perform(multipart("/api/tournaments/{id}/map", tournament.getId())
                        .file(mapData(" ", "Rooms and entrances"))
                        .file(image("venue.png", "image"))
                        .servletPath("/api")
                        .with(authentication(editor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        mockMvc.perform(get("/api/tournaments/{id}/map", tournament.getId()).servletPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tournament map not found"));
    }

    private UsernamePasswordAuthenticationToken organizerWithRole(
            Tournament tournament,
            TournamentRole tournamentRole
    ) {
        String username = "map-organizer-" + UUID.randomUUID();
        User organizer = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Map",
                "Organizer",
                Role.ORGANIZER
        ));

        UserTournamentRole role = new UserTournamentRole();
        role.setId(new UserTournamentKey(organizer.getId(), tournament.getId()));
        role.setUser(organizer);
        role.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        role.setRole(tournamentRole);
        userTournamentRoleRepository.saveAndFlush(role);
        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }

    private static MockMultipartFile mapData(String title, String description) {
        String json = """
                {"title":"%s","description":"%s"}
                """.formatted(title, description);
        return new MockMultipartFile("data", "data", APPLICATION_JSON_VALUE, json.getBytes());
    }

    private static MockMultipartFile mapData(String json) {
        return new MockMultipartFile("data", "data", APPLICATION_JSON_VALUE, json.getBytes());
    }

    private static MockMultipartFile image(String filename, String contents) {
        return new MockMultipartFile("image", filename, "image/png", contents.getBytes());
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Tournament map test");
        tournament.setDescription("Map endpoint fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("userTournamentPermissions");
        }
    }
}
