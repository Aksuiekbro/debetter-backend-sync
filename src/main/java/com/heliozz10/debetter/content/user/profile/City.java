package com.heliozz10.debetter.content.user.profile;

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
@Table(name = "city")
public class City {
    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(analyzer = "edge_ngram")
    @Column(unique = true, nullable = false)
    private String name;

    public City(String name) {
        this.name = name;
    }
}
