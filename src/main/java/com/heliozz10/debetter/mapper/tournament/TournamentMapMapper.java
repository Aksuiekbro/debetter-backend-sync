package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.TournamentMap;
import com.heliozz10.debetter.dto.tournament.in.TournamentMapFormDto;
import com.heliozz10.debetter.dto.tournament.out.TournamentMapView;
import com.heliozz10.debetter.mapper.util.media.UrlMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = UrlMapper.class)
public interface TournamentMapMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    TournamentMap toTournamentMap(TournamentMapFormDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTournamentMap(TournamentMapFormDto dto, @MappingTarget TournamentMap tournamentMap);

    TournamentMapView toTournamentMapView(TournamentMap tournamentMap);
}
