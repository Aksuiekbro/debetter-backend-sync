package com.heliozz10.debetter.controller;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.in.NewsDto;
import com.heliozz10.debetter.dto.out.NewsView;
import com.heliozz10.debetter.service.NewsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NewsMultipartContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @Test
    void createRejectsUpdateOnlyGalleryFields() throws Exception {
        String data = """
                {
                    "title": "Tournament recap",
                    "content": "A recap of the final rounds.",
                    "tags": ["highlights"],
                    "retainedImageIds": [],
                    "newImagePositions": []
                }
                """;

        mockMvc.perform(multipart("/api/news")
                        .file(jsonPart(data))
                        .servletPath("/api")
                        .with(authentication(organizer())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService);
    }

    @Test
    void patchBindsEmptyRetainedImageAndPositionLists() throws Exception {
        String data = """
                {
                    "title": "Tournament recap",
                    "content": "A recap of the final rounds.",
                    "tags": ["highlights"],
                    "retainedImageIds": [],
                    "newImagePositions": []
                }
                """;
        News updatedNews = new News();
        when(newsService.updateNews(any(), eq(null), eq(null), eq(42L), eq(70L))).thenReturn(updatedNews);
        when(newsService.toNewsView(updatedNews)).thenReturn(new NewsView());

        mockMvc.perform(multipart("/api/news/{id}", 42L)
                        .file(jsonPart(data))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .servletPath("/api")
                        .with(authentication(organizer())))
                .andExpect(status().isOk());

        ArgumentCaptor<NewsDto> dtoCaptor = ArgumentCaptor.forClass(NewsDto.class);
        verify(newsService).updateNews(dtoCaptor.capture(), eq(null), eq(null), eq(42L), eq(70L));
        assertEquals(List.of(), dtoCaptor.getValue().retainedImageIds());
        assertEquals(List.of(), dtoCaptor.getValue().newImagePositions());
    }

    private MockMultipartFile jsonPart(String data) {
        return new MockMultipartFile("data", "data", APPLICATION_JSON_VALUE, data.getBytes());
    }

    private UsernamePasswordAuthenticationToken organizer() {
        User user = new User(
                "news-organizer",
                "password",
                "news-organizer@example.invalid",
                "News",
                "Organizer",
                Role.ORGANIZER
        );
        user.setId(7L);
        OrganizerProfile profile = new OrganizerProfile();
        profile.setId(70L);
        profile.setUser(user);
        user.setProfile(profile);
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }
}
