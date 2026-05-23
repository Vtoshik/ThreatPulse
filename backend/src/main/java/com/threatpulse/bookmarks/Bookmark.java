package com.threatpulse.bookmarks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_bookmarks")
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "threat_id", nullable = false)
    private Long threatId;

    @Column(name = "bookmarked_at", nullable = false)
    private OffsetDateTime bookmarkedAt;

    public Bookmark(Long userId, Long threatId) {
        this.userId = userId;
        this.threatId = threatId;
        this.bookmarkedAt = OffsetDateTime.now();
    }
}