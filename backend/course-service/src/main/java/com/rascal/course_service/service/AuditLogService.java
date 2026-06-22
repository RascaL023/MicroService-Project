package com.rascal.course_service.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.entity.AuditLog;
import com.rascal.course_service.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final String SERVICE_NAME = "course-service";

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(
        AuditLogRepository auditLogRepository,
        CurrentUserService currentUserService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAllPaged(
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        LocalDateTime fromTime,
        LocalDateTime toTime,
        Pageable pageable
    ) {
        return auditLogRepository.findAll(
            buildSearchSpec(
                actorUserId,
                normalize(action),
                normalize(entityType),
                normalize(entityId),
                fromTime,
                toTime
            ),
            pageable
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
        String action,
        String entityType,
        Object entityId,
        String description,
        Map<String, ?> metadata
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActorUserId(currentActorId());
            auditLog.setService(SERVICE_NAME);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId == null ? null : String.valueOf(entityId));
            auditLog.setDescription(description);
            auditLog.setMetadataJson(toJson(metadata));
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to write audit log action={} entityType={} entityId={}", action, entityType, entityId, ex);
        }
    }

    private Long currentActorId() {
        try {
            return currentUserService.getUserId();
        } catch (Exception ign) { return null; }
    }

    private String toJson(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;

        return metadata.entrySet().stream()
            .map(entry -> quote(entry.getKey()) + ":" + jsonValue(entry.getValue()))
            .collect(Collectors.joining(",", "{", "}"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;

        return value.trim();
    }

    private Specification<AuditLog> buildSearchSpec(
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        LocalDateTime fromTime,
        LocalDateTime toTime
    ) {
        Specification<AuditLog> spec = (root, query, builder) -> builder.conjunction();

        if (actorUserId != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("actorUserId"), actorUserId));
        }

        if (action != null) {
            String keyword = "%" + action.toLowerCase() + "%";
            spec = spec.and((root, query, builder) -> builder.like(builder.lower(root.get("action")), keyword));
        }

        if (entityType != null) {
            spec = spec.and((root, query, builder) -> builder.equal(builder.lower(root.get("entityType")), entityType.toLowerCase()));
        }

        if (entityId != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("entityId"), entityId));
        }

        if (fromTime != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
        }

        if (toTime != null) {
            spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), toTime));
        }

        return spec;
    }

    private String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .map(this::jsonValue)
                .collect(Collectors.joining(",", "[", "]"));
        }

        return quote(String.valueOf(value));
    }

    private String quote(String value) {
        return "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }

}
