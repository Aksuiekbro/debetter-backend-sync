package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.profile.out.ParticipantProfileView;
import lombok.Data;

@Data
public class SimpleTournamentParticipantView {
    private Long id;
    private Integer speakerScore;
    private ParticipantProfileView participantProfile;
    private SimpleUserView user;
}
