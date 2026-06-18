package com.heliozz10.debetter.content;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.media.Url;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "News.forView",
                attributeNodes = {
                        @NamedAttributeNode(value = "author", subgraph = "authorSubgraph"),
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "authorSubgraph",
                                attributeNodes = {
                                        @NamedAttributeNode("user")
                                }
                        )
                }
        )
})
@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private OrganizerProfile author;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "thumbnail_id")
    private Url thumbnailUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "news_id")
    private List<Url> images;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded(includePaths = {"name"})
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "news_tag",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @Column
    private LocalDateTime timestamp;

    @Column
    private LocalDateTime lastEdited;
}
