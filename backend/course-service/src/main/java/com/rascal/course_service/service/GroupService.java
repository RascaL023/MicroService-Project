package com.rascal.course_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.GroupMapper;
import com.rascal.course_service.dto.mapper.GroupScheduleMapper;
import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.dto.response.GroupDetailResponse;
import com.rascal.course_service.dto.response.GroupMemberResponse;
import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.dto.response.GroupScheduleResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseStatusEnum;
import com.rascal.course_service.repository.EnrollmentRepository;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.GroupScheduleRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseUserCacheService courseUserCacheService;

    public GroupService(
        GroupRepository groupRepository,
        SubjectRepository subjectRepository,
        GroupScheduleRepository groupScheduleRepository,
        EnrollmentRepository enrollmentRepository,
        CourseUserCacheService courseUserCacheService
    ) {
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
        this.groupScheduleRepository = groupScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseUserCacheService = courseUserCacheService;
    }

    @Transactional(readOnly = true)
    public Group getById(Long id) {
        return groupRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getDetailById(Long id) {
        Group group = getById(id);
        List<GroupScheduleResponse> schedules = groupScheduleRepository
            .findByGroupIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(id)
            .stream().map(GroupScheduleMapper::toResponse)
            .toList();

        List<Enrollment> enrollmentsThisGroup = enrollmentRepository
            .findByGroupIdAndDeletedAtIsNullOrderByRoleAscUserIdAsc(id);
        Map<Long, UserLookupResponse> usersById = courseUserCacheService.lookupByIds(
            enrollmentsThisGroup.stream()
                .map(Enrollment::getUserId)
                .toList()
        );
        List<GroupMemberResponse> members = enrollmentsThisGroup.stream()
            .map(enrollment -> toMemberResponse(enrollment, usersById.get(enrollment.getUserId())))
            .toList();

        return new GroupDetailResponse(
            toResponse(group),
            schedules,
            members
        );
    }

    @Transactional(readOnly = true)
    public Page<Group> getAllPaged(
        String name,
        Long subjectId,
        String academicYear,
        Pageable pageable
    ) {
        return groupRepository.searchActiveGroups(
            normalizeSearchName(name),
            subjectId,
            normalizeSearchAcademicYear(academicYear),
            pageable
        );
    }


    public Group create(GroupRequest request) {
        Subject subject = getActiveSubject(request.subjectId());
        String name = normalizeName(request.name());
        String academicYear = normalizeAcademicYear(request.academicYear());

        if (groupRepository.existsBySubjectIdAndAcademicYearAndNameIgnoreCaseAndDeletedAtIsNull(
            subject.getId(),
            academicYear,
            name
        )) { throw new ConflictException("Group already exist"); }

        Group group = GroupMapper.toEntity(
            new Group(), CourseStatusEnum.ON_GOING, 
            name, academicYear, subject
        ); group.setCreatedAt(LocalDateTime.now());

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

        if (groupRepository.existsBySubjectIdAndAcademicYearAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
            subject.getId(),
            academicYear,
            name, id
        )) { throw new ConflictException("Group already exist"); }

        CourseStatusEnum status = request.isDone() == null ? group.getStatus() : 
            request.isDone() ? CourseStatusEnum.PASSED : CourseStatusEnum.ON_GOING;
        GroupMapper.toEntity(group, status, name, academicYear, subject);
        group.setUpdatedAt(LocalDateTime.now());

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

    private GroupResponse toResponse(Group group) {
        return GroupMapper.toResponse(group, group.getSubject());
    }

    private GroupMemberResponse toMemberResponse(Enrollment enrollment, UserLookupResponse user) {
        UserLookupResponse resolvedUser = user == null
            ? new UserLookupResponse(enrollment.getUserId(), null, null, null)
            : user;

        return new GroupMemberResponse(
            enrollment.getId(),
            resolvedUser,
            enrollment.getRole().getDisplayName()
        );
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

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank())
            return "";

        return name.trim();
    }

    private String normalizeSearchAcademicYear(String academicYear) {
        if (academicYear == null || academicYear.isBlank())
            return null;

        return academicYear.trim();
    }

}
