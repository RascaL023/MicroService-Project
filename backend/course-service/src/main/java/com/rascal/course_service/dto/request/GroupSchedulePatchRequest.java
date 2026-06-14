package com.rascal.course_service.dto.request;

import jakarta.validation.constraints.Min;

public record GroupSchedulePatchRequest(
    @Min(value = 1, message = "Invalid Group ID")
    Long groupId,

    String dayOfWeek,

    @Min(value = 1, message = "Invalid schedule template ID")
    Long templateId
) {
    public boolean isEmptyPatch() {
        return groupId == null && dayOfWeek == null && templateId == null;
    }
}
