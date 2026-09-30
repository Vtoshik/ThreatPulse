package com.threatpulse.auth;

import com.threatpulse.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.threatpulse.user.UserRepository;

/**
 * Implementation of Spring Security's UserDetailsService.
 * Loads user-specific data by email for authentication purposes.
 * This service is used by Spring Security to retrieve user details
 * when validating credentials or generating JWT tokens.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmail(email)                                      
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " +     
        email));
    }

    public User loadUserById(Long id) throws UsernameNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }
}
