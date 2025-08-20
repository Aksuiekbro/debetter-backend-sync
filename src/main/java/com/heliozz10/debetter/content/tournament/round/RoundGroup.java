package com.heliozz10.debetter.content.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * This class is needed to group rounds (obviously). There are three types of round groups:
 * Preliminary, Team Elimination and Solo Elimination
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "round_group", indexes = {
        @Index(name = "round_group_tournament_id_fkey", columnList = "tournament_id")
})
public class RoundGroup {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundGroupType type;

    @Enumerated
    @Column(nullable = false)
    private DebateFormat format;

    @OneToMany(mappedBy = "roundGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Round> rounds;

    @Column
    private Integer currentRoundNumber;

    public RoundGroup(Tournament tournament, RoundGroupType type, DebateFormat debateFormat) {
        this.tournament = tournament;
        this.type = type;
        this.format = debateFormat;
    }
}
