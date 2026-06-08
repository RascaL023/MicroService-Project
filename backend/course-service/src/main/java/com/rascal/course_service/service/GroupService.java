package com.rascal.course_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.rascal.course_service.dto.mapper.GroupMapper;
import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseStatusEnum;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;

    public GroupService(
        GroupRepository groupRepository,
        SubjectRepository subjectRepository
    ) {
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
    }

    public Group getById(Long id) {
        return groupRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    public Page<Group> getAll(Pageable pageable) {
        return groupRepository.findByDeletedAtIsNull(pageable);
    }

    public Group create(GroupRequest request) {
        Subject subject = getActiveSubject(request.subjectId());
        String name = normalizeName(request.name());
        String academicYear = normalizeAcademicYear(request.academicYear());

        if (groupRepository.existsBySubject_IdAndAcademicYearAndNameIgnoreCaseAndDeletedAtIsNull(
            subject.getId(),
            academicYear,
            name
        )) { throw new ConflictException("Group already exist"); }

        Group group = GroupMapper.toEntity(
            new GroupRequest(name, subject.getId(), academicYear),
            subject
        );
        group.setStatus(CourseStatusEnum.ON_GOING);
        group.setCreatedAt(LocalDateTime.now());

        return groupRepository.save(group);
    }

    public Group updateById(Long id, GroupPatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        Group group = getById(id);
        Subject subject = request.subjectId() == null ?
            group.getSubject() : getActiveSubject(request.subjectId());
        String name = request.name() == null ? group.getName() : normalizeName(request.name());
        String academicYear = request.academicYear() == null ?
            group.getAcademicYear() : normalizeAcademicYear(request.academicYear());

        if (groupRepository.existsBySubject_IdAndAcademicYearAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
            subject.getId(),
            academicYear,
            name,
            id
        )) { throw new ConflictException("Group already exist"); }

        group.setSubject(subject);
        GroupMapper.updateEntity(group, new GroupPatchRequest(
            name,
            subject.getId(),
            academicYear,
            request.isDone()
        ));

        return groupRepository.save(group);
    }

    public void deleteById(Long id) {
        Group group = getById(id);
        group.setDeletedAt(LocalDateTime.now());

        groupRepository.save(group);
    }

    private Subject getActiveSubject(Long subjectId) {
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Group name must be filled");

        return normalized;
    }

    private String normalizeAcademicYear(String academicYear) {
        String normalized = academicYear.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Academic year must be filled");

        return normalized;
    }
}
