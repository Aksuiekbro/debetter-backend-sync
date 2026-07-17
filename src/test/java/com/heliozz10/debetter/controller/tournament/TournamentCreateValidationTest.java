package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TournamentCreateValidationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createTournamentWithoutEliminationRoundCountReturnsBadRequest() throws Exception {
        String data = """
                {
                    "name": "Validation Cup",
                    "startDate": "%s",
                    "registrationDeadline": "%s",
                    "league": "SCHOOL",
                    "preliminaryFormat": "APF",
                    "teamEliminationFormat": "APF",
                    "preliminaryRoundCount": 3
                }
                """.formatted(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(6));

        mockMvc.perform(multipart("/api/tournaments")
                        .file(new MockMultipartFile("data", "data", APPLICATION_JSON_VALUE, data.getBytes()))
                        .servletPath("/api")
                        .with(authentication(organizer())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("eliminationRoundCount")));
    }

    private UsernamePasswordAuthenticationToken organizer() {
        String username = "organizer-validation-" + UUID.randomUUID();
        User organizer = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "User",
                Role.ORGANIZER
        ));
        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }
}
