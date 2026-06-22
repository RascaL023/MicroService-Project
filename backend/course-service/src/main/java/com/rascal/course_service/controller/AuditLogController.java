package com.rascal.course_service.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.dto.mapper.AuditLogMapper;
import com.rascal.course_service.dto.response.AuditLogResponse;
import com.rascal.course_service.service.AuditLogService;

import id.rascal.response_kit.util.ApiResponse;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('log-access', 'log-access.*')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long actorUserId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
        Pageable pageable
    ) {
        Page<AuditLogResponse> responses = auditLogService
            .getAllPaged(actorUserId, action, entityType, entityId, fromTime, toTime, pageable)
            .map(AuditLogMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }
}
