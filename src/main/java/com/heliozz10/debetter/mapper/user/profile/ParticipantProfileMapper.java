package com.heliozz10.debetter.mapper.user.profile;

import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.user.profile.out.ParticipantProfileView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParticipantProfileMapper {
    ParticipantProfileView toParticipantProfileView(ParticipantProfile profile);
}
