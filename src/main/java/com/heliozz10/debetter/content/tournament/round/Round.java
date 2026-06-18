package com.heliozz10.debetter.content.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.team.Team;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Round.forView",
                attributeNodes = {
                        @NamedAttributeNode("matches")
                }
        ),
        @NamedEntityGraph(
                name = "Round.withTeams",
                attributeNodes = {
                        @NamedAttributeNode("teams")
                }
        )
})
@Entity
@Table(name = "round", indexes = {
        @Index(name = "round_round_group_id_fkey", columnList = "round_group_id")
})
public class Round {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_group_id", nullable = false)
    private RoundGroup roundGroup;

    /**
     * If this field is null then the default format is used (dictated by the round group). Otherwise, the custom format is used
     */
    @Column
    private DebateFormat customFormat;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column
    private Boolean matchesArePublic;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "round_team",
            joinColumns = @JoinColumn(name = "round_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private List<Team> teams;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "round_debater",
            joinColumns = @JoinColumn(name = "round_id"),
            inverseJoinColumns = @JoinColumn(name = "debater_id")
    )
    private List<TournamentParticipant> debaters;

    @OneToMany(mappedBy = "round", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches;

    public Round(RoundGroup roundGroup, String name, int roundNumber) {
        this.roundGroup = roundGroup;
        this.name = name;
        this.roundNumber = roundNumber;
        this.matchesArePublic = false;
    }
}
