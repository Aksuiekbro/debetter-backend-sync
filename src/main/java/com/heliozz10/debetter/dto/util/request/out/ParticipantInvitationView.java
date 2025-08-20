package com.heliozz10.debetter.dto.util.request.out;

import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import com.heliozz10.debetter.dto.user.profile.out.ParticipantProfileView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParticipantInvitationView {
    private Long id;
    private ParticipantProfileView inviter;
    private ParticipantProfileView invitee;
    private TournamentView tournament;
    private TeamView team;
    private LocalDateTime timestamp;
    private Boolean accepted;
}
