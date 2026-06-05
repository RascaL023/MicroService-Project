package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record EnrollmentPatchRequest(
    @Min(value = 1, message = "Invalid User ID")
    Long userId,

    @Pattern(regexp = "(?i)INSTRUCTOR|LEARNER", message = "Invalid course role")
    String role,

    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @Min(value = 1, message = "Invalid Group ID")
    Long groupId
) {
    public boolean isEmptyPatch() {
        return userId == null && role == null && 
            subjectId == null && groupId == null;
    }
}
