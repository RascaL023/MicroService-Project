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

import com.rascal.course_service.dto.mapper.ScheduleTemplateMapper;
import com.rascal.course_service.dto.request.ScheduleTemplatePatchRequest;
import com.rascal.course_service.dto.request.ScheduleTemplateRequest;
import com.rascal.course_service.dto.response.ScheduleTemplateResponse;
import com.rascal.course_service.service.ScheduleTemplateService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schedule-templates")
public class ScheduleTemplateController {

    private final ScheduleTemplateService scheduleTemplateService;

    public ScheduleTemplateController(ScheduleTemplateService scheduleTemplateService) {
        this.scheduleTemplateService = scheduleTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('group-schedule.*', 'group-schedule.read')")
    public ResponseEntity<?> getAllPaged(Pageable pageable) {
        Page<ScheduleTemplateResponse> responses = scheduleTemplateService
            .getAllPaged(pageable)
            .map(ScheduleTemplateMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group-schedule.*', 'group-schedule.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            ScheduleTemplateMapper.toResponse(scheduleTemplateService.getById(id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('group-schedule.*', 'group-schedule.create')")
    public ResponseEntity<?> create(@Valid @RequestBody ScheduleTemplateRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            ScheduleTemplateMapper.toResponse(scheduleTemplateService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group-schedule.*', 'group-schedule.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @Valid @RequestBody ScheduleTemplatePatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            ScheduleTemplateMapper.toResponse(scheduleTemplateService.updateById(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group-schedule.*', 'group-schedule.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        scheduleTemplateService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
