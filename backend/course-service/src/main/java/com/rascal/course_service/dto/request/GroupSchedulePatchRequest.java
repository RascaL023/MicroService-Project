package com.rascal.course_service.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;

public record GroupSchedulePatchRequest(
    @Min(value = 1, message = "Invalid Group ID")
    Long groupId,

    String dayOfWeek,

    LocalTime startTime,

    LocalTime endTime
) {
    public boolean isEmptyPatch() {
        return groupId == null && dayOfWeek == null &&
            startTime == null && endTime == null;
    }
}
