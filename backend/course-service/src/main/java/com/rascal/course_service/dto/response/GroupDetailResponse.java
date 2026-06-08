package com.rascal.course_service.dto.response;

import java.util.List;

public record GroupDetailResponse(
    Long id,
    String name,
    String academicYear,
    String status,
    Long subjectId,
    String subjectName,
    List<UserLookupResponse> members
) { }
