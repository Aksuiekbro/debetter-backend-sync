package com.heliozz10.debetter.content.tournament.announcement;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "announcement")
public class Announcement {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Url imageUrl;

    @Column
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private OrganizerProfile author;

    @OneToMany(mappedBy = "announcement", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @Column
    private Boolean hidden;

    @Column
    private LocalDateTime lastEdited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_editor_id")
    private OrganizerProfile lastEditor;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "announcement_tag",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
}
