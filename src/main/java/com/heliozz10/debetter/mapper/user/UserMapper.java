package com.heliozz10.debetter.mapper.user;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.user.in.UserRegistrationDto;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import com.heliozz10.debetter.mapper.util.socials.SocialProfileMapper;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        SocialProfileMapper.class,
        OrganizerProfileMapper.class,
        ParticipantProfileMapper.class
})
public interface UserMapper {
    @Mapping(target = "password", ignore = true)
    User toUser(UserRegistrationDto dto);

    @Mapping(target = "username", ignore = true)
    void updateUser(UserUpdateDto dto, @MappingTarget User user);

    SimpleUserView toSimpleUserView(User user);

    List<SimpleUserView> toSimpleUserViews(List<User> users);

    @InheritConfiguration(name = "toSimpleUserView")
    @Mapping(target = "profileId", source = "profile.id")
    UserView toUserView(User user);

    List<UserView> toUserViews(List<User> users);
}
