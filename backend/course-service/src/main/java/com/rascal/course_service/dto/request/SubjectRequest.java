package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
    @NotBlank(message = "Subject must be filled")
    @Size(min = 3, max = 50, message = "Subject name is around 3 to 50 characters")
    String name
) { }
