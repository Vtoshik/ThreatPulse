package com.threatpulse.user;

import com.threatpulse.common.domain.Severity;
import com.threatpulse.user.dto.UpdatePreferencesRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserPreferencesRepository userPreferencesRepository;

    @InjectMocks private UserService userService;

    @Test
    void updateTechnologies_shouldSaveNewTechnologies(){
        User user1 = new User("Anna", "anna@example.com", "password");
        user1.setId(1L);

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user1));
        userService.updateTechnologies(user1, Set.of("spring-boot", "kafka"));
        verify(userRepository).save(user1);
    }

    @Test
    void updatePreferences_shouldSaveUpdatedPreferences() {
        User user = new User("Anna", "anna@example.com", "password");
        user.setId(1L);

        UserPreferences existing = new UserPreferences();
        existing.setUser(user);
        existing.setMinSeverity(Severity.HIGH);

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(Severity.CRITICAL, true);

        when(userPreferencesRepository.findByUser(user))
                .thenReturn(Optional.of(existing));

        userService.updatePreferences(user, request);

        verify(userPreferencesRepository).save(existing);
    }

    @Test
    void getProfile_shouldReturnCorrectUserData() {
        User user = new User("Test", "test@test.com", "pass");
        user.setId(1L);
        user.setTechnologies(Set.of("java", "spring"));

        when(userRepository.findById(1L))
                .thenReturn(java.util.Optional.of(user));

        UserProfileResponse response = userService.getProfile(user);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@test.com");
    }
}
