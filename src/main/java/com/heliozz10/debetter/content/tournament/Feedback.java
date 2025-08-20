package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
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
@Table(name = "feedback")
public class Feedback {
    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column
    private LocalDateTime timestamp;

    @Column
    private Boolean edited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private ParticipantProfile author;

    @IndexedEmbedded(includePaths = {"name"})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "feedback_tag",
            joinColumns = @JoinColumn(name = "feedback_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
}
