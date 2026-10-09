package com.fixora.controller;

import com.fixora.dto.response.*;
import com.fixora.service.CatalogService;
import com.fixora.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final CatalogService catalogService;
    private final ReviewService reviewService;

    /** Search and list. Every filter is optional and executed by the database. */
    @GetMapping
    public PageResponseDTO<ServiceResponseDTO> search(@RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) BigDecimal minPrice,
                                                     @RequestParam(required = false) BigDecimal maxPrice,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @RequestParam(defaultValue = "newest") String sort) {
        return catalogService.searchServices(categoryId, keyword, minPrice, maxPrice, page, size, sort);
    }

    /** Alias of GET /api/services kept for a friendlier search URL. */
    @GetMapping("/search")
    public PageResponseDTO<ServiceResponseDTO> searchAlias(@RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) BigDecimal minPrice,
                                                           @RequestParam(required = false) BigDecimal maxPrice,
                                                           @RequestParam(required = false) String city,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "12") int size,
                                                           @RequestParam(defaultValue = "newest") String sort) {
        return catalogService.searchServices(categoryId, keyword, minPrice, maxPrice, page, size, sort);
    }

    @GetMapping("/featured")
    public List<ServiceResponseDTO> featured() {
        return catalogService.featuredServices();
    }

    @GetMapping("/popular")
    public List<ServiceResponseDTO> popular(@RequestParam(defaultValue = "8") int limit) {
        return catalogService.popularServices(limit);
    }

    @GetMapping("/{id}")
    public ServiceResponseDTO get(@PathVariable Long id) {
        return catalogService.getService(id);
    }

    @GetMapping("/{id}/packages")
    public List<ServicePackageResponseDTO> packages(@PathVariable Long id) {
        return catalogService.listPackages(id, false);
    }

    @GetMapping("/{id}/reviews")
    public PageResponseDTO<ReviewResponseDTO> reviews(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return reviewService.reviewsForService(id, page, size);
    }

    @GetMapping("/{id}/rating")
    public RatingSummaryDTO rating(@PathVariable Long id) {
        return reviewService.serviceSummary(id);
    }
}
