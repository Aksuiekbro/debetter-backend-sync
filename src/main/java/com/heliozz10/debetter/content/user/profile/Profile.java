package com.heliozz10.debetter.content.user.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.heliozz10.debetter.content.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Getter
@Setter
@Indexed
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Profile.withUser",
                attributeNodes = {
                        @NamedAttributeNode("user")
                }
        )
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Profile {
    @Id
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @IndexedEmbedded
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;
}