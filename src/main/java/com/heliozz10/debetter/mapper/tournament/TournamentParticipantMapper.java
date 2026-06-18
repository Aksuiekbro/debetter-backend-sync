package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.out.TournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        ParticipantProfileMapper.class
})
public interface TournamentParticipantMapper {
    SimpleTournamentParticipantView toSimpleTournamentParticipantView(TournamentParticipant tournament);

    @InheritConfiguration(name = "toSimpleTournamentParticipantView")
    TournamentParticipantView toTournamentParticipantView(TournamentParticipant tournament);

    //to avoid circular dependency
    @AfterMapping
    default void setTeam(TournamentParticipant tournament, @MappingTarget TournamentParticipantView view, @Autowired TeamMapper teamMapper) {
        if (tournament.getTeam() != null) {
            view.setTeam(teamMapper.toSimpleTeamView(tournament.getTeam()));
        }
    }
}
