package com.fixora.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Transport-safe pagination wrapper (never exposes Spring's Page internals). */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <S, T> PageResponseDTO<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public static <T> PageResponseDTO<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
