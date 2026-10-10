package com.threatpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Extends JpaRepository to provide standard CRUD operations.
 * Nicknames are compared without case, so "Viktor" and "viktor" are the same user.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByUsernameNotIgnoreCase(String username);
}
