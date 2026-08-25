package com.heliozz10.debetter.mapper.util.request;

import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.util.request.out.OrganizerInvitationView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        TournamentMapper.class,
        UserMapper.class
})
public interface OrganizerInvitationMapper {
    @Mapping(source = "inviter.user", target = "inviter")
    @Mapping(source = "invitee.user", target = "invitee")
    OrganizerInvitationView toOrganizerInvitationView(OrganizerInvitation organizerInvitation);

    List<OrganizerInvitationView> toOrganizerInvitationViews(List<OrganizerInvitation> invitations);
}
