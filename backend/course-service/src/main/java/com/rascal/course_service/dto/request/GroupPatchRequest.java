package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupPatchRequest(
    @Size(min = 3, max = 50, message = "Group name is around 3 to 50 characters")
    String name,

    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @Pattern(regexp = "\\d{4}/\\d{4}", message = "Academic year must use format YYYY/YYYY")
    String academicYear,

    Boolean isDone
) {
    public boolean isEmptyPatch() {
        return name == null && subjectId == null &&
            academicYear == null && isDone == null;
    }
}
