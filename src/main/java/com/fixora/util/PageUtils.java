package com.fixora.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Keeps pagination input sane and maps a sort key onto a real database sort. */
public final class PageUtils {

    private static final int MAX_PAGE_SIZE = 60;
    private static final Set<String> ALLOWED_SORTS =
            Set.of("newest", "price_asc", "price_desc", "rating", "name");

    private PageUtils() {
    }

    public static Pageable of(int page, int size) {
        return PageRequest.of(safePage(page), safeSize(size));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(safePage(page), safeSize(size), sort);
    }

    public static int safePage(int page) {
        return Math.max(page, 0);
    }

    public static int safeSize(int size) {
        if (size <= 0) {
            return 12;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** Whitelisted sort keys — arbitrary client sort strings never reach SQL. */
    public static Sort sortFor(String sortKey) {
        String key = sortKey == null ? "newest" : sortKey.toLowerCase();
        if (!ALLOWED_SORTS.contains(key)) {
            key = "newest";
        }
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAverage")
                    .and(Sort.by(Sort.Direction.DESC, "ratingCount"));
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}
