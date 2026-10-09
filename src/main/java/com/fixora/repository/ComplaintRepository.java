package com.fixora.repository;

import com.fixora.entity.Complaint;
import com.fixora.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status, Pageable pageable);

    Optional<Complaint> findByIdAndCustomerId(Long id, Long customerId);

    long countByStatus(ComplaintStatus status);
}
