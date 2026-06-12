package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SubjectMaterialPatchRequest(
    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @Min(value = 1, message = "Meeting number must be at least 1")
    Integer meetingNumber,

    @Size(min = 3, max = 100, message = "Material title is around 3 to 100 characters")
    String title,

    @Size(max = 1000, message = "Material description must be at most 1000 characters")
    String description
) {
    public boolean isEmptyPatch() {
        return subjectId == null && meetingNumber == null &&
            title == null && description == null;
    }
}
