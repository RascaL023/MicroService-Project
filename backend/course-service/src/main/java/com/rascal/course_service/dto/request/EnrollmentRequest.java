package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
    @NotNull(message = "User ID must be filled")
    @Min(value = 1, message = "Invalid User ID")
    Long userId,

    @NotBlank(message = "Course role must be filled")
    String role,

    @NotNull(message = "Subject ID must be filled")
    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @NotNull(message = "Group ID must be filled")
    @Min(value = 1, message = "Invalid Group ID")
    Long GroupId
) { }
