package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.out.TournamentParticipantView;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
        ParticipantProfileMapper.class,
        TeamMapper.class
})
public interface TournamentParticipantMapper {
    SimpleTournamentParticipantView toSimpleTournamentParticipantView(TournamentParticipant tournament);

    List<SimpleTournamentParticipantView> toSimpleTournamentParticipantViews(List<TournamentParticipant> participants);

    @InheritConfiguration(name = "toSimpleTournamentParticipantView")
    TournamentParticipantView toTournamentParticipantView(TournamentParticipant tournament);
}
