package com.heliozz10.debetter.dto.util.request.out;

import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.tournament.team.out.SimpleTeamView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParticipantInvitationView {
    private Long id;
    private SimpleUserView inviter;
    private SimpleUserView invitee;
    private TournamentView tournament;
    private SimpleTeamView team;
    private LocalDateTime timestamp;
    private Boolean accepted;
}
