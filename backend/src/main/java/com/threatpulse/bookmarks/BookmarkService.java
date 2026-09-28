package com.threatpulse.bookmarks;

import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.ThreatResponseMapper;
import com.threatpulse.feed.dto.ThreatResponse;
import com.threatpulse.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;

    public List<ThreatResponse> getBookmarks(User user) {
        List<Long> ids = bookmarkRepository.findByUserId(user.getId())
                .stream().map(Bookmark::getThreatId).toList();
        return threatRepository.findAllById(ids).stream()
                .map(threatResponseMapper::toThreatResponse)
                .toList();
    }

    public Set<Long> getBookmarkedIds(User user) {
        return bookmarkRepository.findByUserId(user.getId())
                .stream().map(Bookmark::getThreatId)
                .collect(Collectors.toSet());
    }

    public void addBookmark(User user, Long threatId) {
        if (!threatRepository.existsById(threatId)) {
            throw new ThreatPulseException("Threat not found", 404);
        }
        if (bookmarkRepository.existsByUserIdAndThreatId(user.getId(), threatId)) {
            return;
        }
        bookmarkRepository.save(new Bookmark(user.getId(), threatId));
    }

    @Transactional
    public void removeBookmark(User user, Long threatId) {
        bookmarkRepository.deleteByUserIdAndThreatId(user.getId(), threatId);
    }
}