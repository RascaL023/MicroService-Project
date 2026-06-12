package com.rascal.course_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.SubjectMaterialMapper;
import com.rascal.course_service.dto.request.SubjectMaterialPatchRequest;
import com.rascal.course_service.dto.request.SubjectMaterialRequest;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectMaterial;
import com.rascal.course_service.repository.SubjectMaterialRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class SubjectMaterialService {

    private final SubjectMaterialRepository subjectMaterialRepository;
    private final SubjectRepository subjectRepository;

    public SubjectMaterialService(
        SubjectMaterialRepository subjectMaterialRepository,
        SubjectRepository subjectRepository
    ) {
        this.subjectMaterialRepository = subjectMaterialRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public Page<SubjectMaterial> getAllPaged(Long subjectId, String title, Pageable pageable) {
        return subjectMaterialRepository.searchActiveMaterials(
            subjectId,
            normalizeSearchTitle(title),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public SubjectMaterial getById(Long id) {
        return subjectMaterialRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Subject material not found"));
    }

    public SubjectMaterial create(SubjectMaterialRequest request) {
        Subject subject = getActiveSubject(request.subjectId());

        rejectDuplicateMeetingNumber(
            subject.getId(),
            request.meetingNumber()
        );

        SubjectMaterial material = SubjectMaterialMapper.toEntity(request, subject);
        material.setTitle(normalizeTitle(request.title()));
        material.setDescription(normalizeDescription(request.description()));
        material.setCreatedAt(LocalDateTime.now());

        return subjectMaterialRepository.save(material);
    }

    public SubjectMaterial updateById(Long id, SubjectMaterialPatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        SubjectMaterial material = getById(id);
        Subject subject = request.subjectId() == null ?
            material.getSubject() : getActiveSubject(request.subjectId());
        Integer meetingNumber = request.meetingNumber() == null ?
            material.getMeetingNumber() : request.meetingNumber();

        rejectDuplicateMeetingNumber(id, subject.getId(), meetingNumber);

        SubjectMaterialMapper.updateEntity(
            material,
            new SubjectMaterialPatchRequest(
                subject.getId(),
                meetingNumber,
                request.title() == null ? material.getTitle() : normalizeTitle(request.title()),
                request.description() == null ?
                    material.getDescription() : normalizeDescription(request.description())
            ),
            subject
        );

        return subjectMaterialRepository.save(material);
    }

    public void deleteById(Long id) {
        SubjectMaterial material = getById(id);
        material.setDeletedAt(LocalDateTime.now());

        subjectMaterialRepository.save(material);
    }

    private Subject getActiveSubject(Long subjectId) {
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private void rejectDuplicateMeetingNumber(Long subjectId, Integer meetingNumber) {
        if (subjectMaterialRepository.existsBySubject_IdAndMeetingNumberAndDeletedAtIsNull(
            subjectId,
            meetingNumber
        )) { throw new ConflictException("Subject material meeting number already exist"); }
    }

    private void rejectDuplicateMeetingNumber(Long id, Long subjectId, Integer meetingNumber) {
        if (subjectMaterialRepository.existsBySubject_IdAndMeetingNumberAndIdNotAndDeletedAtIsNull(
            subjectId,
            meetingNumber,
            id
        )) { throw new ConflictException("Subject material meeting number already exist"); }
    }

    private String normalizeTitle(String title) {
        String normalized = title.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Material title must be filled");

        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) return null;

        String normalized = description.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeSearchTitle(String title) {
        if (title == null || title.isBlank()) return "";

        return title.trim();
    }
}
