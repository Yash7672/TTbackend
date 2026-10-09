package com.fixora.repository;

import com.fixora.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Review> findByServiceIdAndVisibleTrueOrderByCreatedAtDesc(Long serviceId);

    Page<Review> findByServiceIdAndVisibleTrueOrderByCreatedAtDesc(Long serviceId, Pageable pageable);

    Page<Review> findByProviderIdAndVisibleTrueOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByVisibleTrue();

    /** Booking ids that already have a review (batch lookup for booking lists). */
    @Query("select r.booking.id from Review r where r.booking.id in :bookingIds")
    List<Long> findReviewedBookingIds(@Param("bookingIds") List<Long> bookingIds);

    /** Real average only — returns null when there are no reviews. */
    @Query("select avg(r.rating) from Review r where r.service.id = :serviceId and r.visible = true")
    Double averageRatingForService(@Param("serviceId") Long serviceId);

    @Query("select count(r) from Review r where r.service.id = :serviceId and r.visible = true")
    long countForService(@Param("serviceId") Long serviceId);

    @Query("select avg(r.rating) from Review r where r.provider.id = :providerId and r.visible = true")
    Double averageRatingForProvider(@Param("providerId") Long providerId);

    @Query("select count(r) from Review r where r.provider.id = :providerId and r.visible = true")
    long countForProvider(@Param("providerId") Long providerId);

    @Query("select r from Review r where r.visible = true order by r.createdAt desc")
    List<Review> findRecentVisible(Pageable pageable);
}
