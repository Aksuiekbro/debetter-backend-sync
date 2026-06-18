package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.util.socials.SocialProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@Entity
@Table(name = "judge")
public class Judge {
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String fullName;

    @Column
    private String phoneNumber;

    @FullTextField(analyzer = "edge_ngram")
    @Column
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded(includePaths = {"handle"})
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "judge_id")
    private List<SocialProfile> socialProfiles;

    @Column
    private Integer timesJudged;

    @Column(nullable = false)
    private Boolean checkedIn;
}
