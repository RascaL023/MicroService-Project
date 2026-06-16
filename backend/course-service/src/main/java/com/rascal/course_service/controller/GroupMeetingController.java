package com.rascal.course_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.dto.request.GroupMeetingDoneRequest;
import com.rascal.course_service.dto.request.GroupMeetingStartRequest;
import com.rascal.course_service.dto.response.GroupMeetingResponse;
import com.rascal.course_service.service.GroupMeetingService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/group-meetings")
public class GroupMeetingController {

    private final GroupMeetingService groupMeetingService;

    public GroupMeetingController(GroupMeetingService groupMeetingService) {
        this.groupMeetingService = groupMeetingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String status,
        Pageable pageable
    ) {
        Page<GroupMeetingResponse> responses = groupMeetingService
            .getAllPaged(groupId, subjectId, status, pageable);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(HttpStatus.OK, groupMeetingService.getByIdResponse(id));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> start(@Valid @RequestBody GroupMeetingStartRequest request) {
        return ApiResponse.success(HttpStatus.CREATED, groupMeetingService.start(request));
    }

    @PatchMapping("/{id}/done")
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> markDone(
        @PathVariable Long id,
        @RequestBody(required = false) GroupMeetingDoneRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, groupMeetingService.markDone(id, request));
    }
}
