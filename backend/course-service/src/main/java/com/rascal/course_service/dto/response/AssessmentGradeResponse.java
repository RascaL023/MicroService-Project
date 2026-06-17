package com.rascal.course_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentGradeResponse(
    Long id,
    Long assessmentId,
    UserLookupResponse user,
    BigDecimal score,
    String feedback,
    Long gradedBy,
    LocalDateTime gradedAt,
    LocalDateTime updatedAt
) { }
