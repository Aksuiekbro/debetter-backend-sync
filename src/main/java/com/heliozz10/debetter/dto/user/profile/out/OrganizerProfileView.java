package com.heliozz10.debetter.dto.user.profile.out;

import com.heliozz10.debetter.dto.tournament.out.TournamentView;
import lombok.Data;

import java.util.List;

@Data
public class OrganizerProfileView {
    private List<TournamentView> organizedTournaments;
}
