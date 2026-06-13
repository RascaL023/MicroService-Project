package com.rascal.course_service.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.rascal.course_service.dto.mapper.SubjectModuleMapper;
import com.rascal.course_service.dto.response.SubjectModuleResponse;
import com.rascal.course_service.entity.SubjectModule;
import com.rascal.course_service.service.SubjectModuleService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/subject-modules")
public class SubjectModuleController {

    private final SubjectModuleService subjectModuleService;

    public SubjectModuleController(SubjectModuleService subjectModuleService) {
        this.subjectModuleService = subjectModuleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-module.*', 'subject-module.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String filename,
        Pageable pageable
    ) {
        Page<SubjectModuleResponse> responses = subjectModuleService
            .getAllPaged(subjectId, filename, pageable)
            .map(SubjectModuleMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-module.*', 'subject-module.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            SubjectModuleMapper.toResponse(subjectModuleService.getById(id))
        );
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.read', 'subject-module.*', 'subject-module.read')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SubjectModule module = subjectModuleService.getById(id);
        Resource resource = subjectModuleService.getFileResource(module);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(module.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(module.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString()
            )
            .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-module.*', 'subject-module.create')")
    public ResponseEntity<?> create(
        @RequestParam @Min(value = 1, message = "Invalid Subject ID") Long subjectId,
        @RequestParam MultipartFile file
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            SubjectModuleMapper.toResponse(subjectModuleService.create(subjectId, file))
        );
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-module.*', 'subject-module.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @RequestParam(required = false) @Min(value = 1, message = "Invalid Subject ID") Long subjectId,
        @RequestParam(required = false) MultipartFile file
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            SubjectModuleMapper.toResponse(subjectModuleService.updateById(id, subjectId, file))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('subject.*', 'subject.update', 'subject-module.*', 'subject-module.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        subjectModuleService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
