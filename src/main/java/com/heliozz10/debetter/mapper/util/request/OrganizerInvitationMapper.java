package com.heliozz10.debetter.mapper.util.request;

import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.util.request.out.OrganizerInvitationView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        OrganizerProfileMapper.class,
        TournamentMapper.class
})
public interface OrganizerInvitationMapper {
    OrganizerInvitationView toOrganizerInvitationView(OrganizerInvitation organizerInvitation);

    List<OrganizerInvitationView> toOrganizerInvitationViews(List<OrganizerInvitation> invitations);
}
