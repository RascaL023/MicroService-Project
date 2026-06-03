package com.rascal.user_service.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.user_service.dto.mapper.BatchMapper;
import com.rascal.user_service.dto.request.BatchPatchRequest;
import com.rascal.user_service.dto.request.BatchRequest;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.UserRepository;
import com.rascal.user_service.repository.projection.BatchUserCount;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class BatchService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    public BatchService(BatchRepository batchRepository, UserRepository userRepository) {
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Batch> getAllPaged(String name, Pageable pageable) {
        if (name == null || name.isBlank())
            return batchRepository.findAllByDeletedAtIsNull(pageable);

        return batchRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Batch getById(Integer id) {
        return batchRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

    @Transactional(readOnly = true)
    public long countActiveUsers(Integer id) {
        return userRepository.countByBatchIdAndDeletedAtIsNull(id);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> countActiveUsersByBatchIds(Collection<Integer> batchIds) {
        if (batchIds == null || batchIds.isEmpty())
            return Map.of();

        return userRepository.countActiveUsersByBatchIds(batchIds).stream()
            .collect(Collectors.toMap(
                BatchUserCount::getBatchId,
                BatchUserCount::getUserCount
            ));
    }

    public Batch create(BatchRequest request) {
        if (batchRepository.existsByIdAndDeletedAtIsNull(request.id()))
            throw new ConflictException("Batch already exist");

        Batch batch = BatchMapper.toEntity(request);
        if (request.name() != null)
            batch.setName(normalizeName(request.name()));
        batch.setCreatedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    public Batch patch(Integer id, BatchPatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        Batch batch = getById(id);
        if (request.name() != null) batch.setName(normalizeName(request.name()));
        batch.setUpdatedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    public void deleteById(Integer id) {
        Batch batch = getById(id);
        long activeUsers = countActiveUsers(id);

        if (activeUsers > 0)
            throw new ConflictException("Batch still has active users");

        batch.setDeletedAt(LocalDateTime.now());
        batchRepository.save(batch);
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Invalid batch name");

        return normalized;
    }
}
