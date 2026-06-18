package com.heliozz10.debetter.dto.util.request.out;

import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganizerInvitationView {
    private Long id;
    private SimpleUserView inviter;
    private SimpleUserView invitee;
    private TournamentView tournament;
    private LocalDateTime timestamp;
    private Boolean accepted;
}
