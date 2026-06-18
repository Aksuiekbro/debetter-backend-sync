package com.heliozz10.debetter.content.util.socials;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "social_profile")
public class SocialProfile {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialPlatform socialPlatform;

    /**
     * doesnt need @ for TikTok, Instagram etc handles
     */
    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String handle;

    @Column(nullable = false)
    private Boolean isPublic;
}
