package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
                TournamentParticipantMapper.class,
                JudgeMapper.class
        }
)
public interface MatchMapper {
    void receiveMatchResult(MatchResultDto dto, @MappingTarget Match match);

    MatchView toMatchView(Match match);

    List<MatchView> toMatchViews(List<Match> matches);
}
