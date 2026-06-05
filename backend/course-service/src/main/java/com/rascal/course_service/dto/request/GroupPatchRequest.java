package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GroupPatchRequest(
    @Size(min = 3, max = 50, message = "Group name is around 3 to 50 characters")
    String name,

    @Min(value = 1, message = "Invalid batch ID")
    Integer batchId
) {
    public boolean isEmptyPatch() {
        return name == null && batchId == null;
    }
}
