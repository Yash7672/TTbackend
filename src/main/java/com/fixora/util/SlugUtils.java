package com.fixora.util;

import java.text.Normalizer;
import java.util.Locale;

/** Turns display names into URL-safe slugs (used for categories and services). */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "item";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "item" : normalized;
    }

    /** Appends a suffix when the base slug is already taken. */
    public static String withSuffix(String base, long suffix) {
        return base + "-" + suffix;
    }
}
