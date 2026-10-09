package com.fixora.service.impl;

import com.fixora.config.AiProperties;
import com.fixora.dto.request.CategoryRequestDTO;
import com.fixora.dto.request.PackageRequestDTO;
import com.fixora.dto.request.ServiceRequestDTO;
import com.fixora.dto.response.*;
import com.fixora.entity.Category;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.enums.*;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.DuplicateResourceException;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.mapper.CategoryMapper;
import com.fixora.mapper.ServiceMapper;
import com.fixora.repository.*;
import com.fixora.service.AdminService;
import com.fixora.util.PageUtils;
import com.fixora.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ComplaintRepository complaintRepository;
    private final CategoryMapper categoryMapper;
    private final ServiceMapper serviceMapper;
    private final AiProperties aiProperties;

    // ----------------------------------------------------------------- dashboard

    /**
     * Every counter is a live COUNT against MySQL. Nothing here is mocked, and the
     * response includes the AI mode so the admin panel tells the truth about it.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponseDTO dashboard() {
        String aiMode = aiProperties.isAnthropicSelected() && aiProperties.getAnthropic().hasKey()
                ? "anthropic"
                : "fallback";

        return new AdminDashboardResponseDTO(
                userRepository.count(),
                userRepository.countByRole(UserRole.CUSTOMER),
                providerRepository.count(),
                providerRepository.countByStatus(ProviderStatus.APPROVED),
                providerRepository.countByStatus(ProviderStatus.PENDING),
                providerRepository.countByStatus(ProviderStatus.REJECTED),
                providerRepository.countByStatus(ProviderStatus.SUSPENDED),
                categoryRepository.count(),
                serviceRepository.count(),
                packageRepository.count(),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingRepository.countByStatus(BookingStatus.ACCEPTED),
                bookingRepository.countByStatus(BookingStatus.IN_PROGRESS),
                bookingRepository.countByStatus(BookingStatus.COMPLETED),
                bookingRepository.countByStatus(BookingStatus.CANCELLED),
                bookingRepository.countByStatus(BookingStatus.REJECTED),
                reviewRepository.count(),
                complaintRepository.countByStatus(ComplaintStatus.OPEN),
                complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS),
                complaintRepository.countByStatus(ComplaintStatus.RESOLVED),
                complaintRepository.countByStatus(ComplaintStatus.CLOSED),
                aiProperties.getProvider(),
                aiMode
        );
    }

    // ----------------------------------------------------------------- categories

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists.");
        }

        Category category = Category.builder()
                .name(request.name().trim())
                .slug(uniqueCategorySlug(request.name()))
                .description(request.description())
                .iconName(request.iconName())
                .imageUrl(request.imageUrl())
                .active(request.active() == null || request.active())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();

        return categoryMapper.toDto(categoryRepository.save(category), 0);
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));

        String newName = request.name().trim();
        if (!category.getName().equalsIgnoreCase(newName)
                && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new DuplicateResourceException("A category named '" + newName + "' already exists.");
        }

        if (!category.getName().equalsIgnoreCase(newName)) {
            category.setName(newName);
            category.setSlug(uniqueCategorySlug(newName));
        }
        category.setDescription(request.description());
        category.setIconName(request.iconName());
        category.setImageUrl(request.imageUrl());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));

        if (serviceRepository.existsByCategoryId(id)) {
            throw new BadRequestException(
                    "This category still has services. Move or delete them first — Fixora never deletes services implicitly.");
        }
        categoryRepository.delete(category);
    }

    // ------------------------------------------------------------------- services

    @Override
    @Transactional
    public ServiceResponseDTO createService(ServiceRequestDTO request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.categoryId()));

        Service service = Service.builder()
                .name(request.name().trim())
                .slug(uniqueServiceSlug(request.name(), null))
                .shortDescription(request.shortDescription())
                .description(request.description())
                .category(category)
                .imageUrl(request.imageUrl())
                .basePrice(request.basePrice())
                .pricingType(request.pricingType())
                .durationMinutes(request.durationMinutes())
                .active(request.active() == null || request.active())
                .featured(Boolean.TRUE.equals(request.featured()))
                .build();

        return serviceMapper.toDto(serviceRepository.save(service), 0);
    }

    @Override
    @Transactional
    public ServiceResponseDTO updateService(Long id, ServiceRequestDTO request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.categoryId()));

        String newName = request.name().trim();
        if (!service.getName().equalsIgnoreCase(newName)) {
            service.setName(newName);
            service.setSlug(uniqueServiceSlug(newName, id));
        }
        service.setShortDescription(request.shortDescription());
        service.setDescription(request.description());
        service.setCategory(category);
        service.setImageUrl(request.imageUrl());
        service.setBasePrice(request.basePrice());
        service.setPricingType(request.pricingType());
        service.setDurationMinutes(request.durationMinutes());
        if (request.active() != null) {
            service.setActive(request.active());
        }
        if (request.featured() != null) {
            service.setFeatured(request.featured());
        }

        return serviceMapper.toDto(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));

        if (!bookingRepository.findByServiceId(id).isEmpty()) {
            throw new BadRequestException(
                    "This service has bookings. Deactivate it instead of deleting it so history stays intact.");
        }
        if (providerServiceRepository.existsByServiceId(id)) {
            throw new BadRequestException(
                    "Providers still offer this service. Remove those offerings first.");
        }

        packageRepository.deleteByServiceId(id);
        serviceRepository.delete(service);
    }

    // ------------------------------------------------------------------- packages

    @Override
    @Transactional
    public ServicePackageResponseDTO createPackage(PackageRequestDTO request) {
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Service", request.serviceId()));

        ServicePackage pkg = ServicePackage.builder()
                .service(service)
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .durationMinutes(request.durationMinutes())
                .includedWork(request.includedWork())
                .excludedWork(request.excludedWork())
                .pricingType(request.pricingType())
                .active(request.active() == null || request.active())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();

        return serviceMapper.toDto(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public ServicePackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        ServicePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ServicePackage", id));
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Service", request.serviceId()));

        pkg.setService(service);
        pkg.setName(request.name().trim());
        pkg.setDescription(request.description());
        pkg.setPrice(request.price());
        pkg.setDurationMinutes(request.durationMinutes());
        pkg.setIncludedWork(request.includedWork());
        pkg.setExcludedWork(request.excludedWork());
        pkg.setPricingType(request.pricingType());
        if (request.active() != null) {
            pkg.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            pkg.setDisplayOrder(request.displayOrder());
        }

        return serviceMapper.toDto(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        ServicePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ServicePackage", id));

        if (!bookingRepository.findByServicePackageId(id).isEmpty()) {
            throw new BadRequestException(
                    "This package has bookings. Deactivate it instead so existing bookings stay valid.");
        }
        packageRepository.delete(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ServiceResponseDTO> listAllServices(int page, int size) {
        var result = serviceRepository.findAll(PageUtils.of(page, size));
        return new PageResponseDTO<>(serviceMapper.toDtoList(result.getContent()), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    // -------------------------------------------------------------------- helpers

    private String uniqueCategorySlug(String name) {
        String base = SlugUtils.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (categoryRepository.findBySlug(candidate).isPresent()) {
            candidate = SlugUtils.withSuffix(base, suffix++);
        }
        return candidate;
    }

    private String uniqueServiceSlug(String name, Long ignoreId) {
        String base = SlugUtils.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (true) {
            var existing = serviceRepository.findBySlug(candidate);
            if (existing.isEmpty() || (ignoreId != null && existing.get().getId().equals(ignoreId))) {
                return candidate;
            }
            candidate = SlugUtils.withSuffix(base, suffix++);
        }
    }
}
