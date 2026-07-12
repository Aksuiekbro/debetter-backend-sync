package com.heliozz10.debetter.content.tournament.match;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "match_participant_score",
        uniqueConstraints = @UniqueConstraint(
                name = "match_participant_score_match_participant_key",
                columnNames = {"match_id", "participant_id"}
        ),
        indexes = {
                @Index(name = "match_participant_score_match_id_idx", columnList = "match_id"),
                @Index(name = "match_participant_score_participant_id_idx", columnList = "participant_id")
        }
)
public class MatchParticipantScore {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private TournamentParticipant participant;

    @Column(nullable = false)
    private Integer score;
}
