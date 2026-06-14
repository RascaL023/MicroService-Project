package com.rascal.course_service.dto.response;

public record GroupCompleteResponse(
    Long subjectId,
    String academicYear,
    int completedGroups,
    int deletedSchedules
) { }
