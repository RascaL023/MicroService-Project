package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectMaterialRequest(
    @NotNull(message = "Subject ID must be filled")
    @Min(value = 1, message = "Invalid Subject ID")
    Long subjectId,

    @NotNull(message = "Meeting number must be filled")
    @Min(value = 1, message = "Meeting number must be at least 1")
    Integer meetingNumber,

    @NotBlank(message = "Material title must be filled")
    @Size(min = 3, max = 100, message = "Material title is around 3 to 100 characters")
    String title,

    @Size(max = 1000, message = "Material description must be at most 1000 characters")
    String description
) { }
