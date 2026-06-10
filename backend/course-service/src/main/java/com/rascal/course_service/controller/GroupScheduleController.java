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

import com.rascal.course_service.dto.mapper.GroupScheduleMapper;
import com.rascal.course_service.dto.request.GroupSchedulePatchRequest;
import com.rascal.course_service.dto.request.GroupScheduleRequest;
import com.rascal.course_service.dto.response.GroupScheduleResponse;
import com.rascal.course_service.service.GroupScheduleService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/group-schedules")
public class GroupScheduleController {

    private final GroupScheduleService groupScheduleService;

    public GroupScheduleController(GroupScheduleService groupScheduleService) {
        this.groupScheduleService = groupScheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('group.*', 'group.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) String dayOfWeek,
        Pageable pageable
    ) {
        Page<GroupScheduleResponse> responses = groupScheduleService
            .getAllPaged(groupId, dayOfWeek, pageable)
            .map(GroupScheduleMapper::toResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            GroupScheduleMapper.toResponse(groupScheduleService.getById(id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('group.*', 'group.update')")
    public ResponseEntity<?> create(@Valid @RequestBody GroupScheduleRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            GroupScheduleMapper.toResponse(groupScheduleService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @Valid @RequestBody GroupSchedulePatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            GroupScheduleMapper.toResponse(groupScheduleService.updateById(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.update')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        groupScheduleService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
