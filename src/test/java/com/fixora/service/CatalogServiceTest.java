package com.fixora.service;

import com.fixora.BaseDataTest;
import com.fixora.dto.response.ServicePackageResponseDTO;
import com.fixora.dto.response.ServiceResponseDTO;
import com.fixora.entity.Category;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.enums.PricingType;
import com.fixora.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogServiceTest extends BaseDataTest {

    @Autowired private CatalogService catalogService;

    @Test
    void listsOnlyActiveCategories() {
        Category active = category("Active");
        Category hidden = category("Hidden");
        hidden.setActive(false);
        categoryRepository.save(hidden);

        List<Long> ids = catalogService.listCategories(false).stream().map(c -> c.id()).toList();

        assertThat(ids).contains(active.getId());
        assertThat(ids).doesNotContain(hidden.getId());
    }

    @Test
    void returnsPackageCountForEachCategory() {
        Category category = category("Home Cleaning");
        service(category, "Full Home Cleaning", "1999", PricingType.FIXED_PRICE);

        var dto = catalogService.listCategories(false).stream()
                .filter(c -> c.id().equals(category.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(dto.serviceCount()).isEqualTo(1);
    }

    @Test
    void searchFiltersByKeywordInTheDatabase() {
        Category category = category("Plumbing");
        service(category, "Tap Repair", "199", PricingType.STARTING_PRICE);
        service(category, "Tap Installation", "249", PricingType.FIXED_PRICE);

        var results = catalogService.searchServices(null, "Tap Repair", null, null, 0, 20, "newest");

        assertThat(results.content()).isNotEmpty();
        assertThat(results.content()).allMatch(s -> s.name().toLowerCase().contains("tap"));
        assertThat(results.content().stream().map(ServiceResponseDTO::name)).contains("Tap Repair");
    }

    @Test
    void searchFiltersByPriceRange() {
        Category category = category("Mixed");
        service(category, "Cheap Thing", "99", PricingType.FIXED_PRICE);
        service(category, "Expensive Thing", "9999", PricingType.FIXED_PRICE);

        var results = catalogService.searchServices(null, "Thing", new BigDecimal("500"), new BigDecimal("20000"),
                0, 20, "price_asc");

        assertThat(results.content()).isNotEmpty();
        assertThat(results.content()).allMatch(s -> s.basePrice().compareTo(new BigDecimal("500")) >= 0);
        assertThat(results.content()).anyMatch(s -> s.name().equals("Expensive Thing"));
    }

    @Test
    void searchReturnsAnEmptyPageInsteadOfInventingResults() {
        var results = catalogService.searchServices(null, "zzz-no-such-service-zzz", null, null, 0, 20, "newest");

        assertThat(results.content()).isEmpty();
        assertThat(results.totalElements()).isZero();
    }

    @Test
    void listsOnlyActivePackagesOfAService() {
        Category category = category("AC Services");
        Service service = service(category, "AC Deep Cleaning", "799", PricingType.FIXED_PRICE);
        servicePackage(service, "Basic", "799", 90);

        ServicePackage inactive = servicePackage(service, "Retired", "499", 60);
        inactive.setActive(false);
        packageRepository.save(inactive);

        List<ServicePackageResponseDTO> packages = catalogService.listPackages(service.getId(), false);

        assertThat(packages).hasSize(1);
        assertThat(packages.get(0).name()).isEqualTo("Basic");
    }

    @Test
    void unknownServiceRaisesNotFound() {
        assertThatThrownBy(() -> catalogService.getService(987_654L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
