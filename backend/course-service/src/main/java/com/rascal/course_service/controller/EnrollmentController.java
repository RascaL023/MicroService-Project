package com.rascal.course_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.dto.request.EnrollmentPatchRequest;
import com.rascal.course_service.dto.request.EnrollmentRequest;
import com.rascal.course_service.dto.response.EnrollmentResponse;
import com.rascal.course_service.service.EnrollmentService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('enrollment.*', 'enrollment.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String academicYear,
        @RequestParam(required = false) String role,
        Pageable pageable
    ) {
        Page<EnrollmentResponse> responses = enrollmentService
            .getAllPagedResponse(userId, groupId, subjectId, academicYear, role, pageable);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('course.*', 'course.read', 'enrollment.*', 'enrollment.read')")
    public ResponseEntity<?> getMine(Pageable pageable) {
        Page<EnrollmentResponse> responses = enrollmentService.getMineResponse(pageable);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('enrollment.*', 'enrollment.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            enrollmentService.getByIdResponse(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('enrollment.*', 'enrollment.create')")
    public ResponseEntity<?> create(@Valid @RequestBody EnrollmentRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            enrollmentService.createResponse(request)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('enrollment.*', 'enrollment.update')")
    public ResponseEntity<?> patchById(
        @PathVariable Long id,
        @Valid @RequestBody EnrollmentPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            enrollmentService.patchResponse(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('enrollment.*', 'enrollment.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        enrollmentService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
