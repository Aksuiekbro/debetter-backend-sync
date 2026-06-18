package com.heliozz10.debetter.content.tournament;

import com.heliozz10.debetter.content.util.media.Url;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedule")
public class Schedule {
    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 120)
    private String name;

    @Column(length = 5000)
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Url imageUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;
}
