package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Size;

public record GroupPatchRequest(
    @Size(min = 3, max = 50, message = "Group name is around 3 to 50 characters")
    String name
) {
    public boolean isEmptyPatch() {
        return name == null;
    }
}
