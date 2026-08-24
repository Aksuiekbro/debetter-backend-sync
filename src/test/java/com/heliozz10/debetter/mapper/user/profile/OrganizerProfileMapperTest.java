package com.heliozz10.debetter.mapper.user.profile;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizerProfileMapperTest {
    private final OrganizerProfileMapper organizerProfileMapper =
            Mappers.getMapper(OrganizerProfileMapper.class);

    @Test
    void publicProfileOmitsHiddenTournamentsAndKeepsLegacyVisibilityRows() {
        OrganizerProfile profile = new OrganizerProfile();
        profile.setOrganizedTournaments(List.of(
                tournament(1L, false),
                tournament(2L, true),
                tournament(3L, null)
        ));

        OrganizerProfileView view = organizerProfileMapper.toOrganizerProfileView(profile);

        assertEquals(
                List.of(1L, 3L),
                view.getOrganizedTournaments().stream()
                        .map(tournament -> tournament.getId())
                        .toList()
        );
    }

    private Tournament tournament(Long id, Boolean disabled) {
        Tournament tournament = new Tournament();
        tournament.setId(id);
        tournament.setDisabled(disabled);
        return tournament;
    }
}
