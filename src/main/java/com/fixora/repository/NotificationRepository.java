package com.fixora.repository;

import com.fixora.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);
}
