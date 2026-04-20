package com.threatpulse.user;

import com.threatpulse.user.dto.UpdatePreferencesRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public UserProfileResponse getUserInfo(@AuthenticationPrincipal User user){
        return userService.getProfile(user);
    }

    @PutMapping("/technologies")
    public void updateUserTechnologies(@AuthenticationPrincipal User user, @RequestBody Set<String> technologies){
        userService.updateTechnologies(user, technologies);
    }

    @PutMapping("/preferences")
    public void updateUserPreferences(@AuthenticationPrincipal User user, @RequestBody UpdatePreferencesRequest request){
        userService.updatePreferences(user, request);
    }

}
