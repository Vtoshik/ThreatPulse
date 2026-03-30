package com.threatpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Extends JpaRepository to provide standard CRUD operations.
 * Also defines custom query methods for finding a user by email
 * and checking if an email is already in use.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
