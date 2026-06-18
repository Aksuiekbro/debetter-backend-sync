package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.team.in.TeamFormDto;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        TournamentParticipantMapper.class
})
public interface TeamMapper {
    @Mapping(target = "club", ignore = true)
    Team toTeam(TeamFormDto dto);

    SimpleTeamView toSimpleTeamView(Team team);

    List<SimpleTeamView> toSimpleTeamViews(List<Team> teams);

    @InheritConfiguration(name = "toSimpleTeamView")
    TeamView toTeamView(Team team);

    @InheritConfiguration(name = "toSimpleTeamView")
    List<TeamView> toTeamViews(List<Team> teams);
}
