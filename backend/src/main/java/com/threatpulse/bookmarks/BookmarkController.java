package com.threatpulse.bookmarks;

import com.threatpulse.feed.dto.ThreatResponse;
import com.threatpulse.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @GetMapping
    public List<ThreatResponse> getBookmarks(@AuthenticationPrincipal User user) {
        return bookmarkService.getBookmarks(user);
    }

    @GetMapping("/ids")
    public Set<Long> getBookmarkedIds(@AuthenticationPrincipal User user) {
        return bookmarkService.getBookmarkedIds(user);
    }

    @PostMapping("/{threatId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBookmark(@AuthenticationPrincipal User user, @PathVariable Long threatId) {
        bookmarkService.addBookmark(user, threatId);
    }

    @DeleteMapping("/{threatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@AuthenticationPrincipal User user, @PathVariable Long threatId) {
        bookmarkService.removeBookmark(user, threatId);
    }
}