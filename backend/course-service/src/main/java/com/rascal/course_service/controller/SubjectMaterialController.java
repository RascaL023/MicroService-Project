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

import com.rascal.course_service.dto.mapper.SubjectMaterialMapper;
import com.rascal.course_service.dto.request.SubjectMaterialPatchRequest;
import com.rascal.course_service.dto.request.SubjectMaterialRequest;
import com.rascal.course_service.dto.response.SubjectMaterialResponse;
import com.rascal.course_service.service.SubjectMaterialService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subject-materials")
public class SubjectMaterialController {

    private final SubjectMaterialService subjectMaterialService;

    public SubjectMaterialController(SubjectMaterialService subjectMaterialService) {
        this.subjectMaterialService = subjectMaterialService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-material.*', 'subject-material.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String title,
        Pageable pageable
    ) {
        Page<SubjectMaterialResponse> responses = subjectMaterialService
            .getAllPaged(subjectId, title, pageable)
            .map(SubjectMaterialMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-material.*', 'subject-material.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            SubjectMaterialMapper.toResponse(subjectMaterialService.getById(id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-material.*', 'subject-material.create')")
    public ResponseEntity<?> create(@Valid @RequestBody SubjectMaterialRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            SubjectMaterialMapper.toResponse(subjectMaterialService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-material.*', 'subject-material.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @Valid @RequestBody SubjectMaterialPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            SubjectMaterialMapper.toResponse(subjectMaterialService.updateById(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-material.*', 'subject-material.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        subjectMaterialService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
