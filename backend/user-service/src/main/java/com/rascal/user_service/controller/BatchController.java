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

import com.rascal.user_service.dto.mapper.BatchMapper;
import com.rascal.user_service.dto.request.BatchPatchRequest;
import com.rascal.user_service.dto.request.BatchRequest;
import com.rascal.user_service.dto.response.BatchResponse;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.service.BatchService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('batch.*', 'batch.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<Batch> batchPage = batchService.getAllPaged(name, pageable);
        List<Integer> batchIds = batchPage.getContent().stream()
            .map(Batch::getId)
            .toList();
        Map<Integer, Long> userCounts = batchService.countActiveUsersByBatchIds(batchIds);

        Page<BatchResponse> batches = batchPage
            .map(batch -> toResponse(batch, userCounts));

        return ApiResponse.paged(
            HttpStatus.OK,
            batches
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('batch.*', 'batch.read')")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ApiResponse.success(
            HttpStatus.OK,
            toResponse(batchService.getById(id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('batch.*', 'batch.create')")
    public ResponseEntity<?> create(@Valid @RequestBody BatchRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            toResponse(batchService.create(request))
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('batch.*', 'batch.update')")
    public ResponseEntity<?> patchById(
        @PathVariable Integer id,
        @Valid @RequestBody BatchPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            toResponse(batchService.patch(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('batch.*', 'batch.delete')")
    public ResponseEntity<?> deleteById(@PathVariable Integer id) {
        batchService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private BatchResponse toResponse(Batch batch) {
        return BatchMapper.toResponse(
            batch,
            batchService.countActiveUsers(batch.getId())
        );
    }

    private BatchResponse toResponse(Batch batch, Map<Integer, Long> userCounts) {
        return BatchMapper.toResponse(
            batch,
            userCounts.getOrDefault(batch.getId(), 0L)
        );
    }
}
