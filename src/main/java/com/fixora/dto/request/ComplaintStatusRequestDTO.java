package com.fixora.dto.request;

import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.ComplaintPriority;
import com.fixora.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-only complaint triage payload. The admin always makes the final call. */
public record ComplaintStatusRequestDTO(

        @NotNull(message = "status is required")
        ComplaintStatus status,

        ComplaintPriority priority,

        ComplaintCategory category,

        @Size(max = 500, message = "Resolution note must be at most 500 characters")
        String resolutionNote
) {
}
