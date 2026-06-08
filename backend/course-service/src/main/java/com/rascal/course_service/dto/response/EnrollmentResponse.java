package com.rascal.course_service.dto.response;

public record EnrollmentResponse(
    Long id,
    UserLookupResponse user,
    String userRole,
    Long subjectId,
    String subjectName,
    Long groupId,
    String groupName,
    String academicYear
) { }
