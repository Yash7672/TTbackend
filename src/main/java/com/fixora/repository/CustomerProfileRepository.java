package com.fixora.repository;

import com.fixora.entity.CustomerProfile;
import com.fixora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUser(User user);

    Optional<CustomerProfile> findByUserId(Long userId);
}
