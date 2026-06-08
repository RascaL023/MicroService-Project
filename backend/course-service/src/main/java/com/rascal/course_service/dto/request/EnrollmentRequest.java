package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentRequest(
    @NotNull(message = "User ID must be filled")
    @Min(value = 1, message = "Invalid User ID")
    Long userId,

    @NotBlank(message = "Role must be filled")
    @Size(min = 7, max = 10, message = "Invalid Role")
    String role,

    @NotNull(message = "Group ID must be filled")
    @Min(value = 1, message = "Invalid Group ID")
    Long groupId
) { }
