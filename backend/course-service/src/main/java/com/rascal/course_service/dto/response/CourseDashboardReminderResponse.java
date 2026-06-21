package com.rascal.course_service.dto.response;

public record CourseDashboardReminderResponse(
    long learnerPendingAcknowledgements,
    long instructorPendingGrades
) { }
