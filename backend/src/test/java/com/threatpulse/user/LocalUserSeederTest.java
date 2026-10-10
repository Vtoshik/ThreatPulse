package com.threatpulse.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LocalUserSeederTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private LocalUserSeeder seeder(String username, String password) {
        return new LocalUserSeeder(userRepository, passwordEncoder, username, password);
    }

    @Test
    void run_shouldCreateTheAccountWithAHashedPassword() {
        when(userRepository.countByUsernameNotIgnoreCase("demo")).thenReturn(0L);
        when(passwordEncoder.encode("my-own-password")).thenReturn("hash");

        seeder("viktor", "my-own-password").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("viktor");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void run_shouldDoNothing_whenAnAccountOtherThanDemoExists() {
        when(userRepository.countByUsernameNotIgnoreCase("demo")).thenReturn(1L);

        seeder("viktor", "my-own-password").run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_shouldRefuseToStart_whenPasswordIsThePlaceholder() {
        when(userRepository.countByUsernameNotIgnoreCase("demo")).thenReturn(0L);

        assertThatThrownBy(() -> seeder("viktor", "change-me").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TP_PASSWORD");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_shouldRefuseToStart_whenPasswordIsTooShort() {
        when(userRepository.countByUsernameNotIgnoreCase("demo")).thenReturn(0L);

        assertThatThrownBy(() -> seeder("viktor", "short").run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_shouldRefuseToStart_whenUsernameIsMissing() {
        when(userRepository.countByUsernameNotIgnoreCase("demo")).thenReturn(0L);

        assertThatThrownBy(() -> seeder("", "my-own-password").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TP_USERNAME");
    }
}
