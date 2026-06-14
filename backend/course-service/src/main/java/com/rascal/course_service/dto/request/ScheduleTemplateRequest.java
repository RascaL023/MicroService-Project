package com.rascal.course_service.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleTemplateRequest(
    @NotBlank(message = "Template name must be filled")
    @Size(max = 50, message = "Template name must be at most 50 characters")
    String name,

    @NotNull(message = "Start time must be filled")
    LocalTime startTime,

    @NotNull(message = "End time must be filled")
    LocalTime endTime
) { }
