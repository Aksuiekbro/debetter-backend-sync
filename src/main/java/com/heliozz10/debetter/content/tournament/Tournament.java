package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tournament.announcement.Announcement;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@Entity
@Table(name = "tournament")
public class Tournament {
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Url imageUrl;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentLeague league;

    @Column
    private Integer teamLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_organizer_id")
    private OrganizerProfile mainOrganizer;

    @ManyToMany
    @JoinTable(
            name = "tournament_organizer",
            joinColumns = @JoinColumn(name = "tournament_id"),
            inverseJoinColumns = @JoinColumn(name = "organizer_id")
    )
    private List<OrganizerProfile> organizers;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Judge> judges;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Announcement> announcements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebateFormat preliminaryFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebateFormat teamEliminationFormat;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundGroup> roundGroups;

    @OneToMany(mappedBy = "tournament", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules;

    @IndexedEmbedded(includePaths = {"name"})
    @ManyToMany
    @JoinTable(
            name = "tournament_tag",
            joinColumns = @JoinColumn(name = "tournament_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @Column
    private Boolean started;

    @Column
    private Boolean finished;

    @Column
    private Boolean disabled;
}
