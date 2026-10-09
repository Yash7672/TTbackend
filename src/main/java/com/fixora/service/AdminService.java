package com.fixora.service;

import com.fixora.dto.request.CategoryRequestDTO;
import com.fixora.dto.request.PackageRequestDTO;
import com.fixora.dto.request.ServiceRequestDTO;
import com.fixora.dto.response.*;

/** Platform administration: dashboard counters and catalogue management. */
public interface AdminService {

    AdminDashboardResponseDTO dashboard();

    CategoryResponseDTO createCategory(CategoryRequestDTO request);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request);

    void deleteCategory(Long id);

    ServiceResponseDTO createService(ServiceRequestDTO request);

    ServiceResponseDTO updateService(Long id, ServiceRequestDTO request);

    void deleteService(Long id);

    ServicePackageResponseDTO createPackage(PackageRequestDTO request);

    ServicePackageResponseDTO updatePackage(Long id, PackageRequestDTO request);

    void deletePackage(Long id);

    PageResponseDTO<ServiceResponseDTO> listAllServices(int page, int size);
}
