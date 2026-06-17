package com.rascal.course_service.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.course_service.dto.request.AssessmentPatchRequest;
import com.rascal.course_service.dto.request.AssessmentRequest;
import com.rascal.course_service.dto.response.AssessmentResponse;
import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.service.AssessmentService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Long groupMeetingId,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String title,
        Pageable pageable
    ) {
        Page<AssessmentResponse> responses = assessmentService
            .getAllPaged(groupId, groupMeetingId, subjectId, type, title, pageable);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(HttpStatus.OK, assessmentService.getByIdResponse(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Assessment assessment = assessmentService.getById(id);
        Resource resource = assessmentService.getFileResource(id);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(assessment.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(assessment.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString()
            )
            .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> create(
        @RequestParam @Min(value = 1, message = "Invalid Group ID") Long groupId,
        @RequestParam(required = false) @Min(value = 1, message = "Invalid group meeting ID") Long groupMeetingId,
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
        @RequestParam(required = false) MultipartFile file
    ) {
        AssessmentRequest request = new AssessmentRequest(
            groupId,
            groupMeetingId,
            type,
            title,
            description,
            dueAt
        );

        return ApiResponse.success(HttpStatus.CREATED, assessmentService.create(request, file));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @RequestParam(required = false) @Min(value = 1, message = "Invalid Group ID") Long groupId,
        @RequestParam(required = false) @Min(value = 1, message = "Invalid group meeting ID") Long groupMeetingId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
        @RequestParam(required = false) MultipartFile file
    ) {
        AssessmentPatchRequest request = new AssessmentPatchRequest(
            groupId,
            groupMeetingId,
            type,
            title,
            description,
            dueAt
        );

        return ApiResponse.success(HttpStatus.OK, assessmentService.updateById(id, request, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        assessmentService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
