package com.heliozz10.debetter.content.user.role;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_tournament_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "tournament_id"})
})
public class UserTournamentRole {
    @EmbeddedId
    private UserTournamentKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tournamentId")
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    private TournamentRole role;
}
