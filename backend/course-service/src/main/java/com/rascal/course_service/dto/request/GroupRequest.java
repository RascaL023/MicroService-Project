package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GroupRequest(
    @NotBlank(message = "Group name must be filled")
    String name
) { }
