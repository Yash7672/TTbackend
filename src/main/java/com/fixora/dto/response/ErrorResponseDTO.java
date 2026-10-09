package com.fixora.dto.response;

import java.time.Instant;
import java.util.Map;

/** Consistent error body returned by GlobalExceptionHandler for every failure. */
public record ErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ErrorResponseDTO of(int status, String error, String message, String path) {
        return new ErrorResponseDTO(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponseDTO of(int status, String error, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ErrorResponseDTO(Instant.now(), status, error, message, path, fieldErrors);
    }
}
