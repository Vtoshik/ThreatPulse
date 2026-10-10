package com.threatpulse.user;

import com.threatpulse.auth.AuthProperties;
import com.threatpulse.auth.JwtService;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.TechnologyNames;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.user.dto.ChangeCredentialsRequest;
import com.threatpulse.user.dto.UpdatePreferencesRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final JwtService jwtService;

    public UserProfileResponse getProfile(User user){
        User userinfo = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return new UserProfileResponse(
                userinfo.getId(),
                userinfo.getUsername(),
                userinfo.getTechnologies().toArray(new String[0])
        );
    }

    /**
     * Changes the nickname and/or the password of the signed-in user.
     * The hosted demo shares one account between all visitors, so it switches this off.
     */
    @Transactional
    public AuthResponse changeCredentials(User user, ChangeCredentialsRequest request) {
        if (!authProperties.isCredentialsChangeEnabled()) {
            throw new ThreatPulseException("Changing credentials is disabled on this server", 403);
        }
        if (request.newUsername() == null && request.newPassword() == null) {
            throw new ThreatPulseException("Nothing to change", 400);
        }

        User managedUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new ThreatPulseException("User not found", 404)
        );
        if (!passwordEncoder.matches(request.currentPassword(), managedUser.getPasswordHash())) {
            throw new ThreatPulseException("Current password is wrong", 403);
        }

        String newUsername = request.newUsername();
        if (newUsername != null) {
            boolean takenByAnotherUser = !newUsername.equalsIgnoreCase(managedUser.getUsername())
                    && userRepository.existsByUsernameIgnoreCase(newUsername);
            if (takenByAnotherUser) {
                throw new ThreatPulseException("Username already in use", 409);
            }
            managedUser.setUsername(newUsername);
        }
        if (request.newPassword() != null) {
            managedUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            // Every token issued before this moment, for example a stolen one, stops working
            managedUser.setTokenVersion(managedUser.getTokenVersion() + 1);
        }
        userRepository.save(managedUser);
        // The caller's own token is one of the invalidated ones, so hand out a new one
        return new AuthResponse(jwtService.generateToken(managedUser), "Bearer");
    }

    public void updateTechnologies(User user, Set<String> technologies){
        User managedUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        managedUser.setTechnologies(new java.util.HashSet<>(TechnologyNames.normalizeAll(technologies)));
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
