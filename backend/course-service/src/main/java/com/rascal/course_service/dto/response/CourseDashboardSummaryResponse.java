package com.rascal.course_service.dto.response;

public record CourseDashboardSummaryResponse(
    long activeGroups,
    long subjects,
    long instructors,
    long groupsWithoutInstructor,
    long groupsWithoutSchedule
) {}
