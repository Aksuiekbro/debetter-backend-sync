package com.heliozz10.debetter.mapper.user.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizerProfileMapperTest {
    private final OrganizerProfileMapper organizerProfileMapper =
            Mappers.getMapper(OrganizerProfileMapper.class);

    @Test
    void publicProfileListsTournamentsRegardlessOfResultsPublicationStatus() {
        OrganizerProfile profile = new OrganizerProfile();
        profile.setOrganizedTournaments(List.of(
                tournament(1L, false),
                tournament(2L, true),
                tournament(3L, null)
        ));

        OrganizerProfileView view = organizerProfileMapper.toOrganizerProfileView(profile);

        assertEquals(
                List.of(1L, 2L, 3L),
                view.getOrganizedTournaments().stream()
                        .map(tournament -> tournament.getId())
                        .toList()
        );
        assertFalse(view.getOrganizedTournaments().get(0).getDisabled());
        assertTrue(view.getOrganizedTournaments().get(1).getDisabled());
        assertNull(view.getOrganizedTournaments().get(2).getDisabled());
    }

    @Test
    void unpublishedTournamentRemainsVisibleWithoutEmbeddingResults() {
        Tournament tournament = tournament(2L, true);
        tournament.setName("Tournament with unpublished results");
        RoundGroup roundGroup = new RoundGroup();
        roundGroup.setId(10L);
        roundGroup.setTournament(tournament);
        tournament.setRoundGroups(List.of(roundGroup));
        OrganizerProfile profile = new OrganizerProfile();
        profile.setOrganizedTournaments(List.of(tournament));

        OrganizerProfileView view = organizerProfileMapper.toOrganizerProfileView(profile);
        JsonNode json = new ObjectMapper().valueToTree(view);

        assertEquals(1, json.get("organizedTournaments").size());
        JsonNode summary = json.get("organizedTournaments").get(0);
        assertEquals(2L, summary.get("id").asLong());
        assertEquals("Tournament with unpublished results", summary.get("name").asText());
        assertTrue(summary.get("disabled").asBoolean());
        assertFalse(summary.has("roundGroups"));
        assertFalse(summary.has("results"));
    }

    private Tournament tournament(Long id, Boolean disabled) {
        Tournament tournament = new Tournament();
        tournament.setId(id);
        tournament.setDisabled(disabled);
        return tournament;
    }
}
