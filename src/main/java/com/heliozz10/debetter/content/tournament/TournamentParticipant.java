package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@Entity
@Table(name = "tournament_participant")
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    private Integer speakerScore;

    @IndexedEmbedded(includePaths = {
            "user.username",
            "user.firstName",
            "user.lastName",
            "user.email"}
    )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_profile_id")
    private ParticipantProfile participantProfile;
}
