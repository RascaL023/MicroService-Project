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

import com.rascal.course_service.dto.mapper.GroupMapper;
import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.service.GroupService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('group.*', 'group.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) String academicYear,
        Pageable pageable
    ) {
        Page<GroupResponse> responses = groupService
            .getAllPaged(name, subjectId, academicYear, pageable)
            .map(this::groupResponse);

        return ApiResponse.paged(HttpStatus.OK, responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            groupService.getDetailById(id)
        );
    }


    @PostMapping
    @PreAuthorize("hasAnyAuthority('group.*', 'group.create')")
    public ResponseEntity<?> create(@Valid @RequestBody GroupRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            groupResponse(groupService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.update')")
    public ResponseEntity<?> updateById(
        @PathVariable Long id,
        @Valid @RequestBody GroupPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            groupResponse(groupService.updateById(id, request))
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('group.*', 'group.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        groupService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private GroupResponse groupResponse(Group group) {
        return GroupMapper.toResponse(group, group.getSubject());
    }

}
