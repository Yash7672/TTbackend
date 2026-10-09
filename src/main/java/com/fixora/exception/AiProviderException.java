package com.fixora.exception;

/**
 * Thrown by an AI provider when the external call fails or is not configured.
 * The AI service catches this and degrades to deterministic fallback logic, so a
 * missing or broken key never breaks the application.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
