package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.AuditLogResponse;
import com.rascal.course_service.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() { }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getActorUserId(),
            auditLog.getService(),
            auditLog.getAction(),
            auditLog.getEntityType(),
            auditLog.getEntityId(),
            auditLog.getDescription(),
            auditLog.getMetadataJson(),
            auditLog.getCreatedAt()
        );
    }
}
