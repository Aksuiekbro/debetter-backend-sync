package com.heliozz10.debetter.mapper.user.profile;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        Tournament.class,
        UserMapper.class
})
public interface OrganizerProfileMapper {
    OrganizerProfileView toOrganizerProfileView(OrganizerProfile organizerProfile);

    /**
     * Organizer profiles are public-facing wherever they are embedded. Keep
     * hidden tournaments out of every profile serialization while retaining
     * legacy rows whose visibility flag has not been initialized.
     */
    @AfterMapping
    default void hideDisabledTournaments(@MappingTarget OrganizerProfileView view) {
        if (view.getOrganizedTournaments() == null) {
            return;
        }

        view.setOrganizedTournaments(view.getOrganizedTournaments().stream()
                .filter(tournament -> !Boolean.TRUE.equals(tournament.getDisabled()))
                .toList());
    }
}
