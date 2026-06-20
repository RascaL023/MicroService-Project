package com.rascal.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MajorRequest(
    @NotBlank(message = "Major ID must be filled")
    @Pattern(regexp = "^[A-Z0-9_-]{2,20}$", message = "Major ID must use uppercase letters, numbers, underscore, or dash")
    String id,

    @NotBlank(message = "Major name must be filled")
    @Size(max = 100, message = "Major name too long")
    String name
) { }
