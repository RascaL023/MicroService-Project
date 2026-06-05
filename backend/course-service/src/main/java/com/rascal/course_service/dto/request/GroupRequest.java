package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupRequest(
    @NotBlank(message = "Group name must be filled")
    String name,

    @NotNull(message = "Batch ID name must be filled")
    @Min(value = 1, message = "Invalid batch ID")
    Integer batchId
) { }
