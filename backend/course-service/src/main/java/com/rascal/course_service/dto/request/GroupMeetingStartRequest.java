package com.rascal.course_service.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GroupMeetingStartRequest(
    @NotNull(message = "Group ID must be filled")
    @Min(value = 1, message = "Invalid Group ID")
    Long groupId,

    @NotNull(message = "Subject material ID must be filled")
    @Min(value = 1, message = "Invalid subject material ID")
    Long subjectMaterialId,

    LocalDate meetingDate,
    String note
) { }
