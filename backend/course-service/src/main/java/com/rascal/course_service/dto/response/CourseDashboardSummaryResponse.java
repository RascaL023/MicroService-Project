package com.rascal.course_service.dto.response;

public record CourseDashboardSummaryResponse(
    long activeGroups,
    long passedGroups,
    long subjects,
    long instructors,
    long learners,
    long groupsWithoutInstructor,
    long groupsWithoutSchedule
) {}
