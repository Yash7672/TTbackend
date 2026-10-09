package com.fixora.repository;

import com.fixora.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomerIdOrderByDefaultAddressDescIdDesc(Long customerId);

    /** Ownership check: an address is only reachable through its owner. */
    Optional<Address> findByIdAndCustomerId(Long id, Long customerId);

    long countByCustomerId(Long customerId);
}
