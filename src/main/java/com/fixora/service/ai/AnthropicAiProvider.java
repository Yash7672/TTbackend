package com.fixora.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixora.config.AiProperties;
import com.fixora.exception.AiProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional external language model. Selected with AI_PROVIDER=anthropic.
 *
 * The API key lives only on the backend — it is never sent to the browser and
 * never logged. If the key is missing the provider reports itself unavailable and
 * the application silently continues in deterministic fallback mode.
 */
@Slf4j
@Component
public class AnthropicAiProvider implements AiProvider {

    public static final String NAME = "anthropic";

    private static final String SYSTEM_PROMPT = """
            You are the Fixora AI Assistant for a local home-services marketplace.
            Rules you must always follow:
            - Use ONLY the grounding data provided. Never invent prices, provider names, ratings,
              availability, booking status or company policy.
            - If the grounding data does not answer the question, say so and suggest a Fixora page
              or a broader search instead of guessing.
            - Never create or confirm a booking; tell the user to submit it from the service page.
            - Be concise, friendly and practical. Use plain text, short sentences and simple lists.
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AnthropicAiProvider(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 2));
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getAnthropic().getBaseUrl())
                .build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isLlm() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return properties.getAnthropic().hasKey();
    }

    @Override
    public String complete(AiRequest request) {
        if (!isAvailable()) {
            throw new AiProviderException("Anthropic provider is selected but ANTHROPIC_API_KEY is empty.");
        }

        String prompt = buildPrompt(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.resolvedModel());
        body.put("max_tokens", properties.getAnthropic().getMaxTokens());
        body.put("system", SYSTEM_PROMPT);
        body.put("messages", java.util.List.of(Map.of("role", "user", "content", prompt)));

        try {
            String raw = restClient.post()
                    .header("x-api-key", properties.getAnthropic().getApiKey())
                    .header("anthropic-version", properties.getAnthropic().getVersion())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                String text = content.get(0).path("text").asText("");
                if (!text.isBlank()) {
                    return text.trim();
                }
            }
            throw new AiProviderException("Anthropic returned no usable content.");

        } catch (RestClientResponseException ex) {
            // Never log the key; the body is the vendor's own error message.
            log.warn("Anthropic call failed with status {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiProviderException("Anthropic API call failed with status "
                    + ex.getStatusCode().value(), ex);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Anthropic call failed: {}", ex.getMessage());
            throw new AiProviderException("Anthropic API call failed: " + ex.getMessage(), ex);
        }
    }

    private String buildPrompt(AiRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(request.task() == null ? "CHAT" : request.task()).append('\n');
        if (request.instructions() != null && !request.instructions().isBlank()) {
            sb.append("What the answer must contain: ").append(request.instructions()).append('\n');
        }
        if (request.userMessage() != null && !request.userMessage().isBlank()) {
            sb.append("\nUser message:\n").append(request.userMessage()).append('\n');
        }
        sb.append("\nGrounding data (this is the only source of truth):\n");
        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request.facts() == null ? Map.of() : request.facts()));
        } catch (Exception ex) {
            sb.append(String.valueOf(request.facts()));
        }
        return sb.toString();
    }
}
