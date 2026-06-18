package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "TournamentParticipant.forSimpleView",
                attributeNodes = {
                        @NamedAttributeNode(value = "participantProfile", subgraph = "profileSubgraph")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "profileSubgraph",
                                attributeNodes = {
                                        @NamedAttributeNode("user")
                                }
                        )
                }
        ),
        @NamedEntityGraph(
                name = "TournamentParticipant.forView",
                attributeNodes = {
                        @NamedAttributeNode(value = "participantProfile", subgraph = "profileSubgraph"),
                        @NamedAttributeNode("team")
                },
                subgraphs = {
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
@Table(name = "tournament_participant")
public class TournamentParticipant implements Serializable {
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
            "user.email"
    })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_profile_id")
    private ParticipantProfile participantProfile;
}
