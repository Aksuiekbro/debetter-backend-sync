package com.heliozz10.debetter.content.tournament.announcement;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    public Comment(String content, LocalDateTime timestamp, Announcement announcement, User author) {
        this.content = content;
        this.timestamp = timestamp;
        this.announcement = announcement;
        this.author = author;
    }
}
