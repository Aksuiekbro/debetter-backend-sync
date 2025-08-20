package com.heliozz10.debetter.content.user.profile.institution;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Indexed
@Table(name = "institution")
public class Institution {
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(unique = true, nullable = false)
    private String name;

    public Institution(String name) {
        this.name = name;
    }
}
