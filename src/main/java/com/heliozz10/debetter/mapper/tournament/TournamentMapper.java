package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.dto.tournament.in.TournamentFormDto;
import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.mapper.TagMapper;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
                TagMapper.class
        }
)
public interface TournamentMapper {
    Tournament toTournament(TournamentFormDto dto);
    void updateTournament(TournamentFormDto dto, @MappingTarget Tournament tournament);

    TournamentView toTournamentView(Tournament tournament);

    List<TournamentView> toTournamentViews(List<Tournament> tournaments);
}
