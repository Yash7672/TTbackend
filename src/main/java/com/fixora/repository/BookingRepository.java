package com.fixora.repository;

import com.fixora.entity.Booking;
import com.fixora.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingRef(String bookingRef);

    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    Page<Booking> findByCustomerIdOrderByBookingDateDescStartTimeDesc(Long customerId, Pageable pageable);

    Page<Booking> findByCustomerIdAndStatusOrderByBookingDateDesc(Long customerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByProviderIdOrderByBookingDateDescStartTimeDesc(Long providerId, Pageable pageable);

    Page<Booking> findByProviderIdAndStatusOrderByBookingDateAscStartTimeAsc(Long providerId, BookingStatus status, Pageable pageable);

    List<Booking> findByProviderIdAndStatus(Long providerId, BookingStatus status);

    Page<Booking> findByStatusOrderByBookingDateDescStartTimeDesc(BookingStatus status, Pageable pageable);

    List<Booking> findByServiceId(Long serviceId);

    List<Booking> findByServicePackageId(Long packageId);

    List<Booking> findByProviderId(Long providerId);

    List<Booking> findByStatusAndBookingDateBefore(BookingStatus status, LocalDate date);

    long countByStatus(BookingStatus status);

    long countByCustomerId(Long customerId);

    /**
     * Overlap guard for a provider: counts active bookings whose time window
     * intersects the requested window on the same day.
     */
    @Query("""
            select count(b) from Booking b
            where b.provider.id = :providerId
              and b.bookingDate = :date
              and b.status in (com.fixora.enums.BookingStatus.PENDING,
                               com.fixora.enums.BookingStatus.ACCEPTED,
                               com.fixora.enums.BookingStatus.IN_PROGRESS)
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    long countOverlapping(@Param("providerId") Long providerId,
                          @Param("date") LocalDate date,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime);

    @Query("""
            select b from Booking b
            where b.provider is null
              and b.status = com.fixora.enums.BookingStatus.PENDING
            order by b.bookingDate asc, b.startTime asc
            """)
    List<Booking> findUnassignedPending();
}
