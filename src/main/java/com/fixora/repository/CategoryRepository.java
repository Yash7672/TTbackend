package com.fixora.repository;

import com.fixora.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Category> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);
}
