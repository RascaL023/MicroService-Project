package com.rascal.course_service.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupScheduleRequest(
    @NotNull(message = "Group ID must be filled")
    @Min(value = 1, message = "Invalid Group ID")
    Long groupId,

    @NotBlank(message = "Day of week must be filled")
    String dayOfWeek,

    @NotNull(message = "Start time must be filled")
    LocalTime startTime,

    @NotNull(message = "End time must be filled")
    LocalTime endTime
) { }
