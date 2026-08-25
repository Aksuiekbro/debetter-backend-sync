package com.heliozz10.debetter.mapper.util.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.dto.util.request.out.ParticipantInvitationView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ParticipantInvitationMapperTest {
    @Autowired
    private ParticipantInvitationMapper participantInvitationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesInvitationTournamentAndTeam() throws Exception {
        Tournament tournament = new Tournament();
        tournament.setId(101L);
        tournament.setName("Climate Cup");

        Team team = new Team();
        team.setId(202L);
        team.setName("Kaprichoza");
        team.setTournament(tournament);

        ParticipantInvitation invitation = new ParticipantInvitation();
        invitation.setId(303L);
        invitation.setTeam(team);

        ParticipantInvitationView view = participantInvitationMapper.toParticipantInvitationView(invitation);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(view));

        assertEquals(101L, json.path("tournament").path("id").asLong());
        assertEquals("Climate Cup", json.path("tournament").path("name").asText());
        assertEquals(202L, json.path("team").path("id").asLong());
        assertEquals("Kaprichoza", json.path("team").path("name").asText());
    }
}
