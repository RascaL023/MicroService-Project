package com.rascal.user_service.controller;

import java.util.List;
import java.util.Map;

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

import com.rascal.user_service.dto.mapper.MajorMapper;
import com.rascal.user_service.dto.request.MajorPatchRequest;
import com.rascal.user_service.dto.request.MajorRequest;
import com.rascal.user_service.dto.response.MajorResponse;
import com.rascal.user_service.entity.Major;
import com.rascal.user_service.service.MajorService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/majors")
public class MajorController {

    private final MajorService majorService;

    public MajorController(MajorService majorService) {
        this.majorService = majorService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('major.*', 'major.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<Major> majorPage = majorService.getAllPaged(name, pageable);
        List<String> majorIds = majorPage.getContent().stream()
            .map(Major::getId)
            .toList();
        Map<String, Long> userCounts = majorService.countActiveUsersByMajorIds(majorIds);
        Page<MajorResponse> majors = majorPage
            .map(major -> toResponse(major, userCounts));

        return ApiResponse.paged(HttpStatus.OK, majors);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('major.*', 'major.read')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ApiResponse.success(HttpStatus.OK, toResponse(majorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('major.*', 'major.create')")
    public ResponseEntity<?> create(@Valid @RequestBody MajorRequest request) {
        return ApiResponse.success(HttpStatus.CREATED, toResponse(majorService.create(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('major.*', 'major.update')")
    public ResponseEntity<?> patchById(
        @PathVariable String id,
        @Valid @RequestBody MajorPatchRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, toResponse(majorService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('major.*', 'major.delete')")
    public ResponseEntity<?> deleteById(@PathVariable String id) {
        majorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private MajorResponse toResponse(Major major) {
        return MajorMapper.toResponse(major, majorService.countActiveUsers(major.getId()));
    }

    private MajorResponse toResponse(Major major, Map<String, Long> userCounts) {
        return MajorMapper.toResponse(major, userCounts.getOrDefault(major.getId(), 0L));
    }
}
