package com.fixora.repository;

import com.fixora.entity.User;
import com.fixora.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);
}
