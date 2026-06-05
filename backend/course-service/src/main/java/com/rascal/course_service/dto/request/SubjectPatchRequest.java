package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Size;

public record SubjectPatchRequest(
    @Size(min = 3, max = 50, message = "Subject name is around 3 to 50 characters")
    String name
) {
    public boolean isEmptyPatch() {
        return name == null;
    }
}
