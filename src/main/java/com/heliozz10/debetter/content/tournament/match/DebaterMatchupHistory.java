package com.heliozz10.debetter.content.tournament.match;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
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
@Table(name = "debater_matchup_history")
public class DebaterMatchupHistory {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debater1_id")
    private TournamentParticipant debater1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debater2_id")
    private TournamentParticipant debater2;

    @Column
    private Integer timesFaced;

    public DebaterMatchupHistory(TournamentParticipant p1, TournamentParticipant p2, Integer timesFaced) {
        this.debater1 = p1;
        this.debater2 = p2;
        this.timesFaced = timesFaced;
    }
}
