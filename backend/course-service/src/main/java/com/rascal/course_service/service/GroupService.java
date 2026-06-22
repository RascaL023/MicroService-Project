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
import com.rascal.course_service.dto.request.GroupCompleteRequest;
import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.dto.response.GroupCompleteResponse;
import com.rascal.course_service.dto.response.GroupDetailResponse;
import com.rascal.course_service.dto.response.GroupMemberResponse;
import com.rascal.course_service.dto.response.GroupMeetingResponse;
import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.dto.response.GroupScheduleResponse;
import com.rascal.course_service.dto.response.AssessmentResponse;
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
    private final GroupMeetingService groupMeetingService;
    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;

    public GroupService(
        GroupRepository groupRepository,
        SubjectRepository subjectRepository,
        GroupScheduleRepository groupScheduleRepository,
        EnrollmentRepository enrollmentRepository,
        CourseUserCacheService courseUserCacheService,
        GroupMeetingService groupMeetingService,
        AssessmentService assessmentService,
        AuditLogService auditLogService
    ) {
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
        this.groupScheduleRepository = groupScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseUserCacheService = courseUserCacheService;
        this.groupMeetingService = groupMeetingService;
        this.assessmentService = assessmentService;
        this.auditLogService = auditLogService;
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
            .findActiveByGroupIdOrderByDayAndTemplateStartTime(id)
            .stream().map(GroupScheduleMapper::toResponse)
            .toList();
        List<GroupMeetingResponse> meetings = groupMeetingService.getTimelineByGroup(group);
        List<AssessmentResponse> assessments = assessmentService.getByGroupId(group.getId());

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
            meetings,
            assessments,
            members
        );
    }

    @Transactional(readOnly = true)
    public Page<Group> getAllPaged(
        String name,
        Long subjectId,
        String academicYear,
        String status,
        Pageable pageable
    ) {
        return groupRepository.searchActiveGroups(
            normalizeSearchName(name),
            subjectId,
            normalizeSearchAcademicYear(academicYear),
            normalizeSearchStatus(status),
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

        Group saved = groupRepository.save(group);
        auditLogService.log(
            "GROUP_CREATED",
            "GROUP",
            saved.getId(),
            "Created group " + saved.getName(),
            Map.of(
                "name", saved.getName(),
                "subjectId", subject.getId(),
                "academicYear", saved.getAcademicYear()
            )
        );

        return saved;
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

        Group saved = groupRepository.save(group);
        auditLogService.log(
            "GROUP_UPDATED",
            "GROUP",
            saved.getId(),
            "Updated group " + saved.getName(),
            Map.of(
                "name", saved.getName(),
                "subjectId", saved.getSubject().getId(),
                "academicYear", saved.getAcademicYear(),
                "status", saved.getStatus().name()
            )
        );

        return saved;
    }


    public void deleteById(Long id) {
        Group group = getById(id);
        group.setDeletedAt(LocalDateTime.now());

        groupRepository.save(group);
        auditLogService.log(
            "GROUP_DELETED",
            "GROUP",
            group.getId(),
            "Deleted group " + group.getName(),
            Map.of(
                "name", group.getName(),
                "subjectId", group.getSubject().getId(),
                "academicYear", group.getAcademicYear()
            )
        );
    }


    public GroupCompleteResponse completeBySubjectAndAcademicYear(GroupCompleteRequest request) {
        Subject subject = getActiveSubject(request.subjectId());
        String academicYear = normalizeAcademicYear(request.academicYear());
        LocalDateTime now = LocalDateTime.now();

        List<Long> groupIds = groupRepository.findActiveOngoingIdsBySubjectIdAndAcademicYear(
            subject.getId(),
            academicYear
        );
        if (groupIds.isEmpty())
            return new GroupCompleteResponse(subject.getId(), academicYear, 0, 0);

        int deletedSchedules = groupScheduleRepository.deleteByGroupIdIn(groupIds);
        int completedGroups = groupRepository.markPassedByIds(groupIds, now);
        auditLogService.log(
            "GROUPS_COMPLETED",
            "GROUP",
            subject.getId() + ":" + academicYear,
            "Completed groups by subject and academic year",
            Map.of(
                "subjectId", subject.getId(),
                "academicYear", academicYear,
                "completedGroups", completedGroups,
                "deletedSchedules", deletedSchedules,
                "groupIds", groupIds
            )
        );

        return new GroupCompleteResponse(
            subject.getId(),
            academicYear,
            completedGroups,
            deletedSchedules
        );
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

    private CourseStatusEnum normalizeSearchStatus(String status) {
        if (status == null || status.isBlank())
            return null;

        try { return CourseStatusEnum.from(status.trim()); }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid group status");
        }
    }

}
