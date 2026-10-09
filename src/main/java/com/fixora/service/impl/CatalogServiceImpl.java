package com.fixora.service.impl;

import com.fixora.dto.response.*;
import com.fixora.entity.Category;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.mapper.CategoryMapper;
import com.fixora.mapper.ReviewMapper;
import com.fixora.mapper.ServiceMapper;
import com.fixora.repository.*;
import com.fixora.service.CatalogService;
import com.fixora.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryMapper categoryMapper;
    private final ServiceMapper serviceMapper;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> listCategories(boolean includeInactive) {
        List<Category> categories = includeInactive
                ? categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                : categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return categoryMapper.toDtoList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ServiceResponseDTO> searchServices(Long categoryId,
                                                              String keyword,
                                                              BigDecimal minPrice,
                                                              BigDecimal maxPrice,
                                                              int page,
                                                              int size,
                                                              String sort) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw ResourceNotFoundException.of("Category", categoryId);
        }
        String normalisedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Service> result = serviceRepository.search(
                categoryId,
                normalisedKeyword,
                minPrice,
                maxPrice,
                PageUtils.of(page, size, PageUtils.sortFor(sort))
        );

        List<ServiceResponseDTO> content = serviceMapper.toDtoList(result.getContent());
        return new PageResponseDTO<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ServiceResponseDTO> servicesByCategory(Long categoryId, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw ResourceNotFoundException.of("Category", categoryId);
        }
        Page<Service> result = serviceRepository.findByCategoryIdAndActiveTrue(
                categoryId, PageUtils.of(page, size, PageUtils.sortFor("rating")));
        return new PageResponseDTO<>(serviceMapper.toDtoList(result.getContent()), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponseDTO getService(Long id) {
        return serviceMapper.toDto(loadService(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackageResponseDTO> listPackages(Long serviceId, boolean includeInactive) {
        loadService(serviceId);
        List<ServicePackage> packages = includeInactive
                ? packageRepository.findByServiceIdOrderByDisplayOrderAscPriceAsc(serviceId)
                : packageRepository.findByServiceIdAndActiveTrueOrderByDisplayOrderAscPriceAsc(serviceId);
        return serviceMapper.toPackageDtoList(packages);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackageResponseDTO getPackage(Long packageId) {
        ServicePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> ResourceNotFoundException.of("ServicePackage", packageId));
        return serviceMapper.toDto(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponseDTO> featuredServices() {
        return serviceMapper.toDtoList(serviceRepository.findByActiveTrueAndFeaturedTrueOrderByRatingAverageDescIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponseDTO> popularServices(int limit) {
        int capped = Math.min(Math.max(limit, 1), 24);
        List<Service> ordered = serviceRepository.findTop8ByActiveTrueOrderByRatingCountDescRatingAverageDesc();
        return serviceMapper.toDtoList(ordered.stream().limit(capped).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryDTO serviceRatingSummary(Long serviceId) {
        return reviewMapper.toSummary(
                reviewRepository.averageRatingForService(serviceId),
                reviewRepository.countForService(serviceId));
    }

    private Service loadService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));
    }
}
