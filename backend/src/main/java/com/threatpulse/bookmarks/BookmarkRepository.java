package com.threatpulse.bookmarks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserId(Long userId);
    boolean existsByUserIdAndThreatId(Long userId, Long threatId);
    void deleteByUserIdAndThreatId(Long userId, Long threatId);
}