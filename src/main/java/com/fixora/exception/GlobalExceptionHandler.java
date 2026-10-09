package com.fixora.exception;

import com.fixora.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into one consistent ErrorResponseDTO shape.
 * No ResponseEntity is used anywhere — status codes come from @ResponseStatus.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------- 404
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(404, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(404, "Not Found", "No endpoint " + request.getRequestURI(), request.getRequestURI());
    }

    // ---------------------------------------------------------------- 400
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    /** Bean validation failures on @Valid request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = fieldErrors.isEmpty()
                ? "Validation failed"
                : "Validation failed: " + String.join("; ", fieldErrors.entrySet().stream()
                        .map(e -> e.getKey() + " " + e.getValue()).toList());
        return ErrorResponseDTO.of(400, "Bad Request", message, request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return ErrorResponseDTO.of(400, "Bad Request", "Validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String hint = "Request body is missing or malformed. Check field names, date format (yyyy-MM-dd) and time format (HH:mm).";
        return ErrorResponseDTO.of(400, "Bad Request", hint, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(400, "Bad Request",
                "Invalid value for '" + ex.getName() + "'", request.getRequestURI());
    }

    // ---------------------------------------------------------------- 401
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponseDTO handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(401, "Unauthorized", ex.getMessage(), request.getRequestURI());
    }

    // ---------------------------------------------------------------- 403
    @ExceptionHandler(UnauthorizedActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleUnauthorized(UnauthorizedActionException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(403, "Forbidden", ex.getMessage(), request.getRequestURI());
    }

    // ---------------------------------------------------------------- 409
    @ExceptionHandler({DuplicateResourceException.class,
                       BookingConflictException.class,
                       InvalidBookingTransitionException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleConflict(RuntimeException ex, HttpServletRequest request) {
        return ErrorResponseDTO.of(409, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ErrorResponseDTO.of(409, "Conflict",
                "That record conflicts with existing data (duplicate or missing reference).",
                request.getRequestURI());
    }

    // ---------------------------------------------------------------- 500
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleEverythingElse(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return ErrorResponseDTO.of(500, "Internal Server Error",
                "Something went wrong processing the request.", request.getRequestURI());
    }
}
