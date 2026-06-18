package com.heliozz10.debetter.content.tournament.match;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.round.Round;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Match.forView",
                attributeNodes = {
                        @NamedAttributeNode("team1"),
                        @NamedAttributeNode("team2"),
                        @NamedAttributeNode("team3"),
                        @NamedAttributeNode("team4"),
                        @NamedAttributeNode(value = "debater1", subgraph = "debaterSubgraph"),
                        @NamedAttributeNode(value = "debater2", subgraph = "debaterSubgraph"),
                        @NamedAttributeNode("judge")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "debaterSubgraph",
                                attributeNodes = {
                                        @NamedAttributeNode(value = "participantProfile", subgraph = "profileSubgraph")
                                }
                        ),
                        @NamedSubgraph(
                                name = "profileSubgraph",
                                attributeNodes = {
                                        @NamedAttributeNode("user")
                                }
                        )
                }
        )
})
@Entity
@Table(name = "match", indexes = {
        @Index(name = "match_round_id_fkey", columnList = "round_id")
})
public class Match {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    /*
     * Teams
     * In APF and Karl Popper, only team1 and team2 fields are used (team1 vs team2)
     * In BPF all team fields are used (team1 and team2 vs team3 and team4)
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    private Team team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    private Team team2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team3_id")
    private Team team3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team4_id")
    private Team team4;

    /*
     * Debaters
     * These fields are used in the LD format
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debater1_id")
    private TournamentParticipant debater1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debater2_id")
    private TournamentParticipant debater2;

    @Column
    private String location;

    @Column
    private LocalDateTime startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id")
    private Judge judge;

    @Column
    private Integer team1Score;

    @Column
    private Integer team2Score;

    @Column
    private Integer team3Score;

    @Column
    private Integer team4Score;

    @Column
    private Integer debater1Score;

    @Column
    private Integer debater2Score;

    @Column
    private Boolean isBye;

    @Column(nullable = false)
    private Boolean completed;
}
