package com.heliozz10.debetter.dto.util.request.out;

import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganizerInvitationView {
    private Long id;
    private OrganizerProfileView inviter;
    private OrganizerProfileView invitee;
    private TournamentView tournament;
    private LocalDateTime timestamp;
    private Boolean accepted;
}
