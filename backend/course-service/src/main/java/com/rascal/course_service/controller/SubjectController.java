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
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.dto.mapper.SubjectMaterialMapper;
import com.rascal.course_service.dto.mapper.SubjectModuleMapper;
import com.rascal.course_service.dto.mapper.SubjectMapper;
import com.rascal.course_service.dto.request.SubjectPatchRequest;
import com.rascal.course_service.dto.request.SubjectRequest;
import com.rascal.course_service.dto.response.SubjectMaterialResponse;
import com.rascal.course_service.dto.response.SubjectModuleResponse;
import com.rascal.course_service.dto.response.SubjectResponse;
import com.rascal.course_service.service.SubjectMaterialService;
import com.rascal.course_service.service.SubjectModuleService;
import com.rascal.course_service.service.SubjectService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectMaterialService subjectMaterialService;
    private final SubjectModuleService subjectModuleService;

    public SubjectController(
        SubjectService subjectService,
        SubjectMaterialService subjectMaterialService,
        SubjectModuleService subjectModuleService
    ) {
        this.subjectService = subjectService;
        this.subjectMaterialService = subjectMaterialService;
        this.subjectModuleService = subjectModuleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read')")
    public ResponseEntity<?> getAllPaged(Pageable pageable) {
        Page<SubjectResponse> responses = subjectService.getAll(pageable)
            .map(SubjectMapper::toResponse);

        return ApiResponse.paged(
            HttpStatus.OK, 
            responses
        );
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            SubjectMapper.toResponse(subjectService.getById(id))
        );
    }

    @GetMapping("/{id}/materials")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-material.*', 'subject-material.read')")
    public ResponseEntity<?> getMaterials(@PathVariable Long id, Pageable pageable) {
        Page<SubjectMaterialResponse> responses = subjectMaterialService
            .getAllPaged(id, null, pageable)
            .map(SubjectMaterialMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}/modules")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-module.*', 'subject-module.read')")
    public ResponseEntity<?> getModules(@PathVariable Long id, Pageable pageable) {
        Page<SubjectModuleResponse> responses = subjectModuleService
            .getAllPaged(id, null, pageable)
            .map(SubjectModuleMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }


    @PostMapping
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.create')")
    public ResponseEntity<?> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            SubjectMapper.toResponse(subjectService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @Valid @RequestBody SubjectPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            SubjectMapper.toResponse(subjectService.updateById(id, request))
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        subjectService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
    
}
