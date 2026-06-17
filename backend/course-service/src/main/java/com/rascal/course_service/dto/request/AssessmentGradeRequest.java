package com.rascal.course_service.dto.request;

import java.math.BigDecimal;

public record AssessmentGradeRequest(
    Long userId,
    BigDecimal score,
    String feedback
) { }
