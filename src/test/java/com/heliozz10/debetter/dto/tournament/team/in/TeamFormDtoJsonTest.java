package com.heliozz10.debetter.dto.tournament.team.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamFormDtoJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsInvitedParticipantsAsPlainUsernames() throws Exception {
        TeamFormDto dto = objectMapper.readValue("""
                {
                  "name": "kaprichoza",
                  "club": "KTL",
                  "creatorId": 42,
                  "invitedParticipants": ["Arman", "Aisha"]
                }
                """, TeamFormDto.class);

        assertEquals(List.of(
                new ParticipantSelectorDto(null, "Arman"),
                new ParticipantSelectorDto(null, "Aisha")
        ), dto.invitedParticipants());
    }

    @Test
    void acceptsLegacyInvitedParticipantsAsSelectorObjects() throws Exception {
        TeamFormDto dto = objectMapper.readValue("""
                {
                  "name": "kaprichoza",
                  "club": "KTL",
                  "creatorId": 42,
                  "invitedParticipants": [{"username": "Arman"}, {"id": 77}]
                }
                """, TeamFormDto.class);

        assertEquals(List.of(
                new ParticipantSelectorDto(null, "Arman"),
                new ParticipantSelectorDto(77L, null)
        ), dto.invitedParticipants());
    }
}
