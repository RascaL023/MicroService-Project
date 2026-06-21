package com.rascal.course_service.dto.response;

import java.time.LocalDateTime;

public record AssessmentAcknowledgementResponse(
    Long assessmentId,
    Long userId,
    boolean done,
    LocalDateTime doneAt
) { }
