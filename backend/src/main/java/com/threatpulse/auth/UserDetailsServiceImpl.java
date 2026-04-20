package com.threatpulse.auth;

import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Implementation of Spring Security's UserDetailsService.
 * Loads user-specific data by email for authentication purposes.
 * This service is used by Spring Security to retrieve user details
 * when validating credentials or generating JWT tokens.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Retrieve user from database
        Optional<User> user = userRepository.findByEmail(email);

        // Throw exception if user is not found
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        User foundUser = user.get();

        // Map to Spring Security UserDetails with username, password, and authorities
        return org.springframework.security.core.userdetails.User.builder()
                .username(foundUser.getEmail())
                .password(foundUser.getPasswordHash())
                .authorities(Collections.emptyList()) // no roles assigned
                .build();
    }
}
