package com.rascal.course_service.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GroupMeetingResponse(
    Long id,
    Long groupId,
    String groupName,
    Long subjectId,
    String subjectName,
    Long subjectMaterialId,
    Integer meetingNumber,
    String title,
    String description,
    LocalDate meetingDate,
    String status,
    String displayStatus,
    String note,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) { }
