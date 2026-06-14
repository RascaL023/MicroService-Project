package com.rascal.course_service.dto.response;

import java.time.LocalTime;

public record GroupScheduleResponse(
    Long id,
    Long groupId,
    String groupName,
    Long subjectId,
    String subjectName,
    Long templateId,
    String templateName,
    String dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) { }
