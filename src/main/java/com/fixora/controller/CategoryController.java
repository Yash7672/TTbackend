package com.fixora.controller;

import com.fixora.dto.response.CategoryResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.dto.response.ServiceResponseDTO;
import com.fixora.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CatalogService catalogService;

    @GetMapping
    public List<CategoryResponseDTO> list() {
        return catalogService.listCategories(false);
    }

    @GetMapping("/{id}")
    public CategoryResponseDTO get(@PathVariable Long id) {
        return catalogService.getCategory(id);
    }

    @GetMapping("/{id}/services")
    public PageResponseDTO<ServiceResponseDTO> services(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "12") int size) {
        return catalogService.servicesByCategory(id, page, size);
    }
}
