package com.heliozz10.debetter.content.tag;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Indexed
@Entity
@Table(name = "tag", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "type"})
})
public class Tag {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    private TagType type;

    @FullTextField(analyzer = "edge_ngram")
    @Column
    private String name;
}
