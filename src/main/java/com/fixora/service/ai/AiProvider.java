package com.fixora.service.ai;

import java.util.Map;

/**
 * Abstraction over the language-model backend.
 *
 * Two implementations exist:
 *   - {@link FallbackAiProvider}: deterministic, always available, no key needed.
 *   - {@link AnthropicAiProvider}: calls the Anthropic API from the backend only.
 *
 * The application never requires a provider to be reachable: every call site is
 * wrapped so that a failure degrades to the deterministic implementation.
 */
public interface AiProvider {

    /** Stable identifier, e.g. "fallback" or "anthropic". */
    String name();

    /** True only for a real language model. Used to label responses honestly. */
    boolean isLlm();

    /** False when the provider is selected but not configured (e.g. missing key). */
    boolean isAvailable();

    /**
     * Renders an answer.
     *
     * The {@code facts} map carries ONLY data read from the Fixora database
     * (real services, real packages, real counts). Providers must never invent a
     * price, provider name, rating, slot or policy that is not in there.
     */
    String complete(AiRequest request);

    /**
     * @param task         CHAT | BOOKING_ASSISTANCE | SEARCH | DESCRIPTION | COMPLAINT_CLASSIFICATION
     * @param userMessage  the raw user text (may be null for admin-driven tasks)
     * @param instructions what the answer should contain
     * @param facts        grounded data from the database
     */
    record AiRequest(String task, String userMessage, String instructions, Map<String, Object> facts) {
    }
}
