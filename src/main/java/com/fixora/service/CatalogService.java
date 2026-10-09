package com.fixora.service;

import com.fixora.dto.response.*;

import java.math.BigDecimal;
import java.util.List;

/** Read-side of the service catalogue: categories, services, packages, search. */
public interface CatalogService {

    List<CategoryResponseDTO> listCategories(boolean includeInactive);

    CategoryResponseDTO getCategory(Long id);

    PageResponseDTO<ServiceResponseDTO> searchServices(Long categoryId,
                                                      String keyword,
                                                      BigDecimal minPrice,
                                                      BigDecimal maxPrice,
                                                      int page,
                                                      int size,
                                                      String sort);

    PageResponseDTO<ServiceResponseDTO> servicesByCategory(Long categoryId, int page, int size);

    ServiceResponseDTO getService(Long id);

    List<ServicePackageResponseDTO> listPackages(Long serviceId, boolean includeInactive);

    ServicePackageResponseDTO getPackage(Long packageId);

    List<ServiceResponseDTO> featuredServices();

    List<ServiceResponseDTO> popularServices(int limit);

    RatingSummaryDTO serviceRatingSummary(Long serviceId);
}
