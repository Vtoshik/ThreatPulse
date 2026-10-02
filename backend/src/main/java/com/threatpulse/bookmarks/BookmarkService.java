package com.threatpulse.bookmarks;

import com.threatpulse.common.domain.AnalysisStatus;
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

    // The mapping reads the lazy technologies of each threat, which needs an open session
    @Transactional(readOnly = true)
    public List<ThreatResponse> getBookmarks(User user) {
        List<Long> ids = bookmarkRepository.findByUserId(user.getId())
                .stream().map(Bookmark::getThreatId).toList();
        // A pending threat has no analysis yet, so it is not shown even if it was bookmarked
        return threatRepository.findAllById(ids).stream()
                .filter(threat -> threat.getAnalysisStatus() == AnalysisStatus.ANALYZED)
                .map(threatResponseMapper::toThreatResponse)
                .toList();
    }

    public Set<Long> getBookmarkedIds(User user) {
        return bookmarkRepository.findByUserId(user.getId())
                .stream().map(Bookmark::getThreatId)
                .collect(Collectors.toSet());
    }

    public void addBookmark(User user, Long threatId) {
        if (!threatRepository.existsByIdAndAnalysisStatus(threatId, AnalysisStatus.ANALYZED)) {
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