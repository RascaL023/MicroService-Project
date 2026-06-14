package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GroupCompleteRequest(
    @NotNull(message = "Subject ID must be filled")
    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @NotBlank(message = "Academic year must be filled")
    @Pattern(regexp = "\\d{4}/\\d{4}", message = "Academic year must use format YYYY/YYYY")
    String academicYear
) { }
