package com.rascal.course_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.dto.request.AssessmentGradeBatchRequest;
import com.rascal.course_service.service.AssessmentGradeService;

import id.rascal.response_kit.util.ApiResponse;

@RestController
@RequestMapping("/api/assessments/{assessmentId}/grades")
public class AssessmentGradeController {

    private final AssessmentGradeService assessmentGradeService;

    public AssessmentGradeController(AssessmentGradeService assessmentGradeService) {
        this.assessmentGradeService = assessmentGradeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'enrollment.*', 'group.read')")
    public ResponseEntity<?> getByAssessmentId(@PathVariable Long assessmentId) {
        return ApiResponse.success(
            HttpStatus.OK,
            assessmentGradeService.getByAssessmentId(assessmentId)
        );
    }

    @PatchMapping
    // @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'enrollment.*', 'group.update')")
    public ResponseEntity<?> upsertBatch(
        @PathVariable Long assessmentId,
        @RequestBody AssessmentGradeBatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            assessmentGradeService.upsertBatch(assessmentId, request)
        );
    }
}
