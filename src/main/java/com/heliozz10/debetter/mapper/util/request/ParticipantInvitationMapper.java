package com.heliozz10.debetter.mapper.util.request;

import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import com.heliozz10.debetter.dto.util.request.out.ParticipantInvitationView;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        ParticipantProfileMapper.class,
        TournamentMapper.class
})
public interface ParticipantInvitationMapper {
    @Mapping(target = "tournament", expression = "java(tournamentMapper.toTournamentView(participantInvitation.getTournament()))")
    ParticipantInvitationView toParticipantInvitationView(ParticipantInvitation participantInvitation);

    List<ParticipantInvitationView> toParticipantInvitationViews(List<ParticipantInvitation> participantInvitations);
}
