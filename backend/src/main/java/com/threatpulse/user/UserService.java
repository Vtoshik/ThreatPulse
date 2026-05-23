package com.threatpulse.user;

import com.threatpulse.common.domain.Severity;
import com.threatpulse.user.dto.UpdatePreferencesRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    public UserProfileResponse getProfile(User user){
        User userinfo = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return new UserProfileResponse(
                userinfo.getId(),
                userinfo.getUsername(),
                userinfo.getEmail(),
                userinfo.getTechnologies().toArray(new String[0])
        );
    }

    public void updateTechnologies(User user, Set<String> technologies){
        User managedUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        managedUser.setTechnologies(technologies);
        userRepository.save(managedUser);
    }

    public void updatePreferences(User user, UpdatePreferencesRequest request){
        User managedUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        UserPreferences userPreferences = userPreferencesRepository.findByUser(managedUser)
                .orElseGet(() -> {
                    UserPreferences prefs = new UserPreferences();
                    prefs.setUser(managedUser);
                    prefs.setMinSeverity(Severity.HIGH);
                    return prefs;
                });

        userPreferences.setMinSeverity(request.minSeverity());
        userPreferences.setEmailAlertsEnabled(request.emailAlertsEnabled());
        userPreferencesRepository.save(userPreferences);
    }


}
