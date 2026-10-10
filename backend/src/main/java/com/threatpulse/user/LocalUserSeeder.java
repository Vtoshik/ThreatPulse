package com.threatpulse.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the single account of a local installation on first start.
 * <p>
 * The nickname and the password come from the environment, never from the code. The
 * application refuses to start with an empty or a placeholder password, because a
 * well-known default password on a security tool is a vulnerability. The user can
 * change both values later in the settings.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.local-user.enabled", havingValue = "true")
public class LocalUserSeeder implements ApplicationRunner {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final String PLACEHOLDER = "change-me";
    private static final String DEMO_USERNAME = "demo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public LocalUserSeeder(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.local-user.username:}") String username,
                           @Value("${app.local-user.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        // A migration creates the shared demo account on every new database, so it
        // does not count. Any other account wins: its password may have been changed.
        if (userRepository.countByUsernameNotIgnoreCase(DEMO_USERNAME) > 0) {
            return;
        }
        if (username.isBlank()) {
            throw new IllegalStateException("TP_USERNAME must be set for a local installation");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || PLACEHOLDER.equals(password)) {
            throw new IllegalStateException(
                    "TP_PASSWORD must be set to your own password of at least "
                            + MIN_PASSWORD_LENGTH + " characters (not the placeholder)");
        }
        userRepository.save(new User(username, passwordEncoder.encode(password)));
        log.info("Created the local account '{}'", username);
    }
}
