package com.heliozz10.debetter.content.tournament.team;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.util.List;


/**
 * IMPORTANT: this class describes teams that exist in a context of a specific tournament
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team")
public class Team {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<TournamentParticipant> members;

    @Column
    private Integer preliminaryScore;

    /**
     * Is true when all the participants of the team have accepted the invitation
     */
    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean checkedIn;

    @Column
    private Boolean disqualified;
}
