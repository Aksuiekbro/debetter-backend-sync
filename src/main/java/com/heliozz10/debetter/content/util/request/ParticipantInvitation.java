package com.heliozz10.debetter.content.util.request;

import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participant_invitation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"inviter_id", "invitee_id", "team_id"})
})
public class ParticipantInvitation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private ParticipantProfile inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id")
    private ParticipantProfile invitee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column
    private LocalDateTime timestamp;

    @Column
    private Boolean accepted;
}
