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
