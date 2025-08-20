package com.heliozz10.debetter.mapper.user;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.dto.user.in.UserRegistrationDto;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.dto.user.profile.out.ProfileView;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        OrganizerProfileMapper.class,
        ParticipantProfileMapper.class
})
public interface UserMapper {
    User toUser(UserRegistrationDto dto);

    @Mapping(target = "username", ignore = true)
    void updateUser(UserUpdateDto dto, @MappingTarget User user);

    @ObjectFactory
    default ProfileView createProfileView(Profile profile, OrganizerProfileMapper organizerProfileMapper, ParticipantProfileMapper participantProfileMapper) {
        if(profile instanceof OrganizerProfile organizerProfile) {
            return organizerProfileMapper.toOrganizerProfileView(organizerProfile);
        } else if (profile instanceof ParticipantProfile participantProfile) {
            return participantProfileMapper.toParticipantProfileView(participantProfile);
        }
        throw new IllegalArgumentException("Unknown profile type");
    }

    SimpleUserView toSimpleUserView(User user);

    List<SimpleUserView> toSimpleUserViews(List<User> users);

    @InheritConfiguration(name = "toSimpleUserView")
    UserView toUserView(User user);

    List<UserView> toUserViews(List<User> users);
}
