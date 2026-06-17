package com.rascal.course_service.dto.request;

import java.util.List;

public record AssessmentGradeBatchRequest(
    List<AssessmentGradeRequest> grades
) { }
