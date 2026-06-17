package com.rascal.course_service.dto.request;

import java.time.LocalDateTime;

public record AssessmentRequest(
    Long groupId,
    Long groupMeetingId,
    String type,
    String title,
    String description,
    LocalDateTime dueAt
) { }
