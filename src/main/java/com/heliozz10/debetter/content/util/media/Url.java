package com.heliozz10.debetter.content.util.media;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "url")
public class Url {
    @Id
    @GeneratedValue
    private long id;

    /**
     * Can be relative or absolute.
     * For example, in the case of an image, which is stored on the server, the url will be relative: `/path/to/image.jpg`
     * If the image is stored on a CDN, the url will be absolute: `https://cdn.example.com/path/to/image.jpg`
     */
    @Column(nullable = false)
    private String url;
}
