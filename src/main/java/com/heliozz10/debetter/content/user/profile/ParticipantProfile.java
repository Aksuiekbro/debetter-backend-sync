package com.heliozz10.debetter.content.user.profile;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.Feedback;
import com.heliozz10.debetter.content.user.profile.institution.Institution;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participant_profile")
public class ParticipantProfile extends Profile implements Serializable {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(nullable = false)
    private Integer rating = 0;

    @OneToMany(mappedBy = "participantProfile")
    private List<TournamentParticipant> teamMemberships;

    @OneToMany(mappedBy = "author")
    private List<Feedback> feedbacks;

    public ParticipantProfile(City city, Institution institution) {
        this.city = city;
        this.institution = institution;
    }
}
