package com.rascal.course_service.dto.response;

import java.time.LocalTime;

public record ScheduleTemplateResponse(
    Long id,
    String name,
    LocalTime startTime,
    LocalTime endTime
) { }
