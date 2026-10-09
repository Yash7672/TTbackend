package com.fixora.config;

import com.fixora.entity.Category;
import com.fixora.entity.Provider;
import com.fixora.entity.Service;
import com.fixora.repository.CategoryRepository;
import com.fixora.repository.ProviderRepository;
import com.fixora.repository.ServiceRepository;
import com.fixora.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Points demo image URLs at the local artwork that ships in the frontend
 * (`/images/categories/*.svg`, `/images/services/*.svg`, `/images/providers/*.svg`).
 *
 * It only touches rows whose image URL is empty or still points at the old
 * random picsum.photos placeholders, so:
 *   - a database seeded before the artwork existed is repaired on startup,
 *   - a fresh database is left alone (the seeder already writes the new paths),
 *   - an administrator's or provider's own image URL is never overwritten.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DemoImageLinkInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = 0;

        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            if (needsRepair(category.getImageUrl()) && category.getSlug() != null) {
                category.setImageUrl("/images/categories/" + category.getSlug() + ".svg");
                categoryRepository.save(category);
                updated++;
            }
        }

        List<Service> services = serviceRepository.findAll();
        for (Service service : services) {
            if (needsRepair(service.getImageUrl()) && service.getSlug() != null) {
                service.setImageUrl("/images/services/" + service.getSlug() + ".svg");
                serviceRepository.save(service);
                updated++;
            }
        }

        List<Provider> providers = providerRepository.findAll();
        for (Provider provider : providers) {
            if (needsRepair(provider.getProfileImageUrl()) && provider.getBusinessName() != null) {
                provider.setProfileImageUrl(
                        "/images/providers/" + SlugUtils.slugify(provider.getBusinessName()) + ".svg");
                providerRepository.save(provider);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("Pointed {} demo images at the local Fixora artwork.", updated);
        }
    }

    private boolean needsRepair(String url) {
        return url == null || url.isBlank() || url.contains("picsum.photos");
    }
}
