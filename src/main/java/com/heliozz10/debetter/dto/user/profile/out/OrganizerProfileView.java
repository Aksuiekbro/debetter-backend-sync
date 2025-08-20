package com.heliozz10.debetter.dto.user.profile.out;

import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentView;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrganizerProfileView extends ProfileView {
    private Long id;
    private List<SimpleTournamentView> organizedTournaments;
    private List<SimpleTournamentView> coOrganizedTournaments;
    private SimpleUserView user;
}
