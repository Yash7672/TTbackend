package com.fixora.dto.response;

/** Every number here is counted from MySQL at request time — nothing is fabricated. */
public record AdminDashboardResponseDTO(
        long totalUsers,
        long totalCustomers,
        long totalProviders,
        long approvedProviders,
        long pendingProviders,
        long rejectedProviders,
        long suspendedProviders,
        long totalCategories,
        long totalServices,
        long totalPackages,
        long totalBookings,
        long pendingBookings,
        long acceptedBookings,
        long inProgressBookings,
        long completedBookings,
        long cancelledBookings,
        long rejectedBookings,
        long totalReviews,
        long openComplaints,
        long inProgressComplaints,
        long resolvedComplaints,
        long closedComplaints,
        String aiProvider,
        String aiMode
) {
}
