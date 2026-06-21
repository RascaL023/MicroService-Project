package com.rascal.course_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentResponse(
    Long id,
    Long groupId,
    String groupName,
    Long subjectId,
    String subjectName,
    Long groupMeetingId,
    Long subjectMaterialId,
    Integer meetingNumber,
    String meetingTitle,
    String type,
    String typeLabel,
    String title,
    String description,
    BigDecimal weight,
    LocalDateTime dueAt,
    boolean hasFile,
    String originalFilename,
    String filePath,
    String mimeType,
    Long fileSize,
    boolean acknowledged,
    LocalDateTime acknowledgedAt
) { }
