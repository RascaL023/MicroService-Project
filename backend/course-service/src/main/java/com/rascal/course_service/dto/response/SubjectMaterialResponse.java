package com.rascal.course_service.dto.response;

public record SubjectMaterialResponse(
    Long id,
    Long subjectId,
    String subjectName,
    Integer meetingNumber,
    String title,
    String description
) { }
