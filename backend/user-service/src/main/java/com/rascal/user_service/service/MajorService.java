package com.rascal.user_service.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.user_service.dto.mapper.MajorMapper;
import com.rascal.user_service.dto.request.MajorPatchRequest;
import com.rascal.user_service.dto.request.MajorRequest;
import com.rascal.user_service.entity.Major;
import com.rascal.user_service.repository.MajorRepository;
import com.rascal.user_service.repository.UserRepository;
import com.rascal.user_service.repository.projection.MajorUserCount;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class MajorService {

    private final MajorRepository majorRepository;
    private final UserRepository userRepository;

    public MajorService(MajorRepository majorRepository, UserRepository userRepository) {
        this.majorRepository = majorRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Major> getAllPaged(String name, Pageable pageable) {
        if (name == null || name.isBlank())
            return majorRepository.findAllByDeletedAtIsNull(pageable);

        return majorRepository.searchActiveMajors(name.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Major getById(String id) {
        return majorRepository.findByIdAndDeletedAtIsNull(normalizeId(id))
            .orElseThrow(() -> new NotFoundException("Major not found"));
    }

    @Transactional(readOnly = true)
    public long countActiveUsers(String id) {
        return userRepository.countByMajorIdAndDeletedAtIsNull(normalizeId(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countActiveUsersByMajorIds(Collection<String> majorIds) {
        if (majorIds == null || majorIds.isEmpty())
            return Map.of();

        return userRepository.countActiveUsersByMajorIds(majorIds).stream()
            .collect(Collectors.toMap(
                MajorUserCount::getMajorId,
                MajorUserCount::getUserCount
            ));
    }

    public Major create(MajorRequest request) {
        String id = normalizeId(request.id());
        if (majorRepository.existsByIdAndDeletedAtIsNull(id))
            throw new ConflictException("Major already exist");

        Major major = MajorMapper.toEntity(request);
        major.setId(id);
        major.setName(normalizeName(request.name()));
        major.setCreatedAt(LocalDateTime.now());

        return majorRepository.save(major);
    }

    public Major patch(String id, MajorPatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        Major major = getById(id);
        if (request.name() != null) major.setName(normalizeName(request.name()));
        major.setUpdatedAt(LocalDateTime.now());

        return majorRepository.save(major);
    }

    public void deleteById(String id) {
        Major major = getById(id);
        long activeUsers = countActiveUsers(id);

        if (activeUsers > 0)
            throw new ConflictException("Major still has active users");

        major.setDeletedAt(LocalDateTime.now());
        majorRepository.save(major);
    }

    private String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim().toUpperCase();
        if (normalized.isBlank())
            throw new BadRequestException("Invalid major ID");

        return normalized;
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Invalid major name");

        return normalized;
    }

}
