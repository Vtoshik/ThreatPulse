package com.threatpulse.user;

import com.threatpulse.auth.AuthProperties;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.user.dto.ChangeCredentialsRequest;
import com.threatpulse.user.dto.UpdatePreferencesRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserPreferencesRepository userPreferencesRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.threatpulse.auth.JwtService jwtService;
    @Spy private AuthProperties authProperties = new AuthProperties();

    @InjectMocks private UserService userService;

    @BeforeEach
    void enableCredentialChanges() {
        authProperties.setCredentialsChangeEnabled(true);
    }

    @Test
    void updateTechnologies_shouldSaveNewTechnologies(){
        User user1 = new User("Anna", "password");
        user1.setId(1L);

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user1));
        userService.updateTechnologies(user1, Set.of("spring-boot", "kafka"));
        verify(userRepository).save(user1);
    }

    @Test
    void updatePreferences_shouldSaveUpdatedPreferences() {
        User user = new User("Anna", "password");
        user.setId(1L);

        UserPreferences existing = new UserPreferences();
        existing.setUser(user);
        existing.setMinSeverity(Severity.HIGH);

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(Severity.CRITICAL, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser(user))
                .thenReturn(Optional.of(existing));

        userService.updatePreferences(user, request);

        verify(userPreferencesRepository).save(existing);
    }

    @Test
    void getProfile_shouldReturnCorrectUserData() {
        User user = new User("Test", "pass");
        user.setId(1L);
        user.setTechnologies(Set.of("java", "spring"));

        when(userRepository.findById(1L))
                .thenReturn(java.util.Optional.of(user));

        UserProfileResponse response = userService.getProfile(user);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("Test");
        assertThat(response.technologies()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void changeCredentials_shouldBeRejected_whenTheServerDisablesIt() {
        authProperties.setCredentialsChangeEnabled(false);
        User user = new User("demo", "hash");
        user.setId(1L);

        assertThatThrownBy(() -> userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", null, "new-password-1")))
                .isInstanceOf(ThreatPulseException.class)
                .satisfies(e -> assertThat(((ThreatPulseException) e).getStatusCode()).isEqualTo(403));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeCredentials_shouldRejectWrongCurrentPassword() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changeCredentials(user,
                new ChangeCredentialsRequest("wrong", null, "new-password-1")))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Current password is wrong");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeCredentials_shouldStoreHashOfNewPassword() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password-1")).thenReturn("new-hash");

        when(jwtService.generateToken(user)).thenReturn("fresh-token");

        var response = userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", null, "new-password-1"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getUsername()).isEqualTo("anna");
        verify(userRepository).save(user);
        // The version moves on, so tokens issued before the change stop working,
        // and the caller receives a token for the new version
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(response.getAccessToken()).isEqualTo("fresh-token");
    }

    @Test
    void changeCredentials_shouldKeepTokenVersion_whenOnlyTheNicknameChanges() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("annie")).thenReturn(false);

        userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", "annie", null));

        assertThat(user.getTokenVersion()).isZero();
    }

    @Test
    void changeCredentials_shouldRejectNicknameOfAnotherUser() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("bob")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", "bob", null)))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Username already in use");

        assertThat(user.getUsername()).isEqualTo("anna");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeCredentials_shouldAllowChangingOnlyTheCaseOfOwnNickname() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);

        userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", "Anna", null));

        // The nickname lookup would find the user itself, which must not count as "taken"
        assertThat(user.getUsername()).isEqualTo("Anna");
        verify(userRepository).save(user);
    }

    @Test
    void changeCredentials_shouldRejectRequestWithNothingToChange() {
        User user = new User("anna", "stored-hash");
        user.setId(1L);

        assertThatThrownBy(() -> userService.changeCredentials(user,
                new ChangeCredentialsRequest("old-password", null, null)))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Nothing to change");
    }
}
