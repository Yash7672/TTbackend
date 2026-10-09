package com.fixora.repository;

import com.fixora.entity.ProviderAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, Long> {

    List<ProviderAvailability> findByProviderIdOrderByDayOfWeekAscStartTimeAsc(Long providerId);

    List<ProviderAvailability> findByProviderIdAndDayOfWeekAndActiveTrue(Long providerId, DayOfWeek dayOfWeek);

    void deleteByProviderId(Long providerId);
}
