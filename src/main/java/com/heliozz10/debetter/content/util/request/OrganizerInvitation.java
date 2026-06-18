package com.heliozz10.debetter.content.util.request;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "OrganizerInvitation.forView",
                attributeNodes = {
                        @NamedAttributeNode(value = "inviter", subgraph = "profileSubgraph"),
                        @NamedAttributeNode(value = "invitee", subgraph = "profileSubgraph"),
                        @NamedAttributeNode(value = "tournament")
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
                name = "OrganizerInvitation.withInviteeAndTournament",
                attributeNodes = {
                        @NamedAttributeNode(value = "invitee", subgraph = "profileSubgraph"),
                        @NamedAttributeNode(value = "tournament")
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
@Table(name = "organizer_invitation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"inviter_id", "invitee_id", "tournament_id"})
})
public class OrganizerInvitation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private OrganizerProfile inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id")
    private OrganizerProfile invitee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column
    private LocalDateTime timestamp;

    @Column
    private Boolean accepted;
}
