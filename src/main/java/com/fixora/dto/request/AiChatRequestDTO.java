package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Free-text message sent to the Fixora AI Assistant. */
public record AiChatRequestDTO(

        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city
) {
}
