package com.heliozz10.debetter.content.tournament.match;

import com.heliozz10.debetter.content.tournament.team.Team;
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
@Table(name = "team_matchup_history", uniqueConstraints = {
        @UniqueConstraint(name = "uc_teammatchuphistory", columnNames = {"team1_id", "team2_id"})
})
public class TeamMatchupHistory {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    private Team team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    private Team team2;

    @Column
    private Integer timesFaced;

    public TeamMatchupHistory(Team t1, Team t2, Integer timesFaced) {
        this.team1 = t1;
        this.team2 = t2;
        this.timesFaced = timesFaced;
    }
}
