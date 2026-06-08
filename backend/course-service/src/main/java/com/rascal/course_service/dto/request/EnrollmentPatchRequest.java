package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record EnrollmentPatchRequest(
    @Min(value = 1, message = "Invalid User ID")
    Long userId,

    @Size(min = 7, max = 10, message = "Invalid Role")
    String role,

    @Min(value = 1, message = "Invalid Group ID")
    Long groupId
) { 
    public boolean isEmptyPatch() {
        return userId == null && role == null &&
            groupId == null;
    }
}
