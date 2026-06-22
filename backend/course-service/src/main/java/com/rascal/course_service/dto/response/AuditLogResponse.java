package com.rascal.course_service.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String service,
    String action,
    String entityType,
    String entityId,
    String description,
    String metadataJson,
    LocalDateTime createdAt
) { }
