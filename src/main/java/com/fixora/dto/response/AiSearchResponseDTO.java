package com.fixora.dto.response;

import java.util.List;
import java.util.Map;

/** Natural-language search result: the parsed criteria plus real catalogue matches. */
public record AiSearchResponseDTO(
        String query,
        Map<String, Object> criteria,
        String mode,
        String message,
        List<ServiceResponseDTO> services
) {
}
