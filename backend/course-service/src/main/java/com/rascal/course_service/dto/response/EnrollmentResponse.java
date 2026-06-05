package com.rascal.course_service.dto.response;

public record EnrollmentResponse(
    Long id,
    String username,
    String role,
    String subjectName,
    String groupName
) { }
