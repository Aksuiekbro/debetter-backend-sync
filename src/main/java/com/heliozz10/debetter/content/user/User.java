package com.heliozz10.debetter.content.user;

import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.content.util.socials.SocialProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

//TODO: add rating
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "User.forView",
                attributeNodes = {
                        @NamedAttributeNode("profile"),
                        @NamedAttributeNode("socialProfiles")
                }
        ),
        @NamedEntityGraph(
                name = "User.forSecurity",
                attributeNodes = {
                        @NamedAttributeNode("authorities"),
                        @NamedAttributeNode("profile")
                }
        )
})
@Entity
@Table(name = "_user")
public class User implements UserDetails {
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String password;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String firstName;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false)
    private String lastName;

    @FullTextField(analyzer = "edge_ngram")
    @Column(nullable = false, unique = true)
    private String email;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Url imageUrl;

    @ManyToMany
    @JoinTable(
            name = "user_authority",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private List<Authority> authorities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded(includePaths = {"handle"})
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<SocialProfile> socialProfiles;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime usernameLastEditedAt;

    public User(String username, String password, String email, String firstName, String lastName, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
