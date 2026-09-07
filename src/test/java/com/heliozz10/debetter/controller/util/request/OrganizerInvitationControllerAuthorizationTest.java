package com.heliozz10.debetter.controller.util.request;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.util.request.out.OrganizerInvitationView;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import com.heliozz10.debetter.service.util.request.OrganizerInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizerInvitationControllerAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizerInvitationService organizerInvitationService;

    @MockitoBean
    private TournamentSecurity tournamentSecurity;

    private User organizer;

    @BeforeEach
    void setUp() {
        organizer = new User(
                "organizer-inviter",
                "password",
                "organizer-inviter@example.invalid",
                "Main",
                "Organizer",
                Role.ORGANIZER
        );
        organizer.setId(7L);

        OrganizerProfile profile = new OrganizerProfile();
        profile.setId(70L);
        profile.setUser(organizer);
        organizer.setProfile(profile);
    }

    @Test
    void organizerWithEditOnlyPermissionCannotSendInvitation() throws Exception {
        when(tournamentSecurity.hasFullPermission(organizer, 42L)).thenReturn(false);

        mockMvc.perform(post("/api/organizer-invitations")
                        .servletPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteeUsername":"cohost","tournamentId":42}
                                """)
                        .with(authentication(organizerAuthentication())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(organizerInvitationService);
    }

    @Test
    void organizerWithFullPermissionCanSendInvitation() throws Exception {
        OrganizerInvitation invitation = new OrganizerInvitation();
        when(tournamentSecurity.hasFullPermission(organizer, 42L)).thenReturn(true);
        when(organizerInvitationService.createInvitation(70L, "cohost", 42L)).thenReturn(invitation);
        when(organizerInvitationService.toOrganizerInvitationView(invitation))
                .thenReturn(new OrganizerInvitationView());

        mockMvc.perform(post("/api/organizer-invitations")
                        .servletPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteeUsername":"cohost","tournamentId":42}
                                """)
                        .with(authentication(organizerAuthentication())))
                .andExpect(status().isOk());

        verify(tournamentSecurity).hasFullPermission(organizer, 42L);
        verify(organizerInvitationService).createInvitation(70L, "cohost", 42L);
        verify(organizerInvitationService).toOrganizerInvitationView(any(OrganizerInvitation.class));
    }

    private UsernamePasswordAuthenticationToken organizerAuthentication() {
        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }
}
