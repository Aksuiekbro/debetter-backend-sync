package com.heliozz10.debetter.mapper.user.profile;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {
        Tournament.class,
        UserMapper.class
})
public interface OrganizerProfileMapper {
    OrganizerProfileView toOrganizerProfileView(OrganizerProfile organizerProfile);
}
