package com.heliozz10.debetter.content.user.profile;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organizer_profile")
public class OrganizerProfile extends Profile {
    @OneToMany(mappedBy = "mainOrganizer", fetch = FetchType.LAZY)
    private List<Tournament> organizedTournaments;

    @ManyToMany(mappedBy = "organizers", fetch = FetchType.LAZY)
    private List<Tournament> coOrganizedTournaments;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "inviter", fetch = FetchType.LAZY)
    private List<OrganizerInvitation> invitations;

    @OneToMany(mappedBy = "invitee", fetch = FetchType.LAZY)
    private List<OrganizerInvitation> receivedInvitations;
}
