package com.rascal.course_service.dto.request;

import java.time.LocalDateTime;

public record AssessmentPatchRequest(
    Long groupId,
    Long groupMeetingId,
    String type,
    String title,
    String description,
    LocalDateTime dueAt
) {
    public boolean isEmptyPatch() {
        return groupId == null
            && groupMeetingId == null
            && type == null
            && title == null
            && description == null
            && dueAt == null;
    }
}
