package com.rascal.course_service.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Size;

public record ScheduleTemplatePatchRequest(
    @Size(max = 50, message = "Template name must be at most 50 characters")
    String name,

    LocalTime startTime,

    LocalTime endTime
) {
    public boolean isEmptyPatch() {
        return name == null && startTime == null && endTime == null;
    }
}
