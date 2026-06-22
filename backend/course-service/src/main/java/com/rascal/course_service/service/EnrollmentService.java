package com.rascal.course_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.EnrollmentMapper;
import com.rascal.course_service.dto.request.EnrollmentPatchRequest;
import com.rascal.course_service.dto.request.EnrollmentRequest;
import com.rascal.course_service.dto.response.EnrollmentResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.CourseUserCache;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.enumerated.CourseRoleEnum;
import com.rascal.course_service.repository.EnrollmentRepository;
import com.rascal.course_service.repository.GroupRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;
    private final CurrentUserService currentUserService;
    private final CourseUserCacheService courseUserCacheService;
    private final AuditLogService auditLogService;

    public EnrollmentService(
        EnrollmentRepository enrollmentRepository,
        GroupRepository groupRepository,
        CurrentUserService currentUserService,
        CourseUserCacheService courseUserCacheService,
        AuditLogService auditLogService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.groupRepository = groupRepository;
        this.currentUserService = currentUserService;
        this.courseUserCacheService = courseUserCacheService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Enrollment> getAllPaged(
        Long userId,
        Long groupId,
        Long subjectId,
        String academicYear,
        String role,
        Pageable pageable
    ) {
        return enrollmentRepository.searchActiveEnrollments(
            userId,
            groupId,
            subjectId,
            normalizeSearchAcademicYear(academicYear),
            normalizeSearchRole(role),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getAllPagedResponse(
        Long userId,
        Long groupId,
        Long subjectId,
        String academicYear,
        String role,
        Pageable pageable
    ) {
        return toResponsePage(getAllPaged(userId, groupId, subjectId, academicYear, role, pageable));
    }

    @Transactional(readOnly = true)
    public Page<Enrollment> getMine(Pageable pageable) {
        return getAllPaged(currentUserService.getUserId(), null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMineResponse(Pageable pageable) {
        return toResponsePage(getMine(pageable));
    }

    @Transactional(readOnly = true)
    public Enrollment getById(Long id) {
        return enrollmentRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Enrollment not found"));
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getByIdResponse(Long id) {
        return toResponse(getById(id));
    }

    public Enrollment create(EnrollmentRequest request) {
        Group group = getActiveGroup(request.groupId());
        CourseRoleEnum role = normalizeRole(request.role());

        requireEnrollableUser(request.userId(), role);
        rejectDuplicateEnrollment(request.userId(), group.getId());
        rejectSecondInstructor(group.getId(), role);

        Enrollment enrollment = EnrollmentMapper.toEntity(request, group);
        enrollment.setCreatedAt(LocalDateTime.now());

        Enrollment saved = enrollmentRepository.save(enrollment);
        auditLogService.log(
            "ENROLLMENT_CREATED",
            "ENROLLMENT",
            saved.getId(),
            "Created enrollment",
            Map.of(
                "userId", saved.getUserId(),
                "groupId", saved.getGroup().getId(),
                "role", saved.getRole().name()
            )
        );

        return saved;
    }

    public EnrollmentResponse createResponse(EnrollmentRequest request) {
        return toResponse(create(request));
    }

    public Enrollment patch(Long id, EnrollmentPatchRequest request) {
        if (request.isEmptyPatch()) throw new BadRequestException("Invalid patch");

        Enrollment enrollment = getById(id);
        CourseRoleEnum role = request.role() == null ? enrollment.getRole() : normalizeRole(request.role());
        Long targetGroupId = request.groupId() == null ? enrollment.getGroup().getId() : request.groupId();
        Long targetUserId = request.userId() == null ? enrollment.getUserId() : request.userId();
        boolean membershipChanged =
            !targetUserId.equals(enrollment.getUserId()) ||
            !targetGroupId.equals(enrollment.getGroup().getId()) ||
            role != enrollment.getRole();

        if (membershipChanged)
            requireEnrollableUser(targetUserId, role);
        if (role == CourseRoleEnum.INSTRUCTOR
            && (enrollment.getRole() != CourseRoleEnum.INSTRUCTOR || !targetGroupId.equals(enrollment.getGroup().getId())))
            rejectSecondInstructor(targetGroupId, role);
        if (!targetUserId.equals(enrollment.getUserId()) || !targetGroupId.equals(enrollment.getGroup().getId()))
            rejectDuplicateEnrollment(targetUserId, targetGroupId);

        EnrollmentMapper.updateEntity(
            enrollment, 
            request, 
            request.groupId() == null ? null : getActiveGroup(request.groupId())
        );

        Enrollment saved = enrollmentRepository.save(enrollment);
        auditLogService.log(
            "ENROLLMENT_UPDATED",
            "ENROLLMENT",
            saved.getId(),
            "Updated enrollment",
            Map.of(
                "userId", saved.getUserId(),
                "groupId", saved.getGroup().getId(),
                "role", saved.getRole().name()
            )
        );

        return saved;
    }

    public EnrollmentResponse patchResponse(Long id, EnrollmentPatchRequest request) {
        return toResponse(patch(id, request));
    }

    public void deleteById(Long id) {
        Enrollment enrollment = getById(id);
        enrollment.setDeletedAt(LocalDateTime.now());

        enrollmentRepository.save(enrollment);
        auditLogService.log(
            "ENROLLMENT_DELETED",
            "ENROLLMENT",
            enrollment.getId(),
            "Deleted enrollment",
            Map.of(
                "userId", enrollment.getUserId(),
                "groupId", enrollment.getGroup().getId(),
                "role", enrollment.getRole().name()
            )
        );
    }

    private Group getActiveGroup(Long groupId) {
        return groupRepository.findByIdAndDeletedAtIsNull(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    private Page<EnrollmentResponse> toResponsePage(Page<Enrollment> enrollments) {
        Map<Long, UserLookupResponse> usersById = lookupUsersById(
            enrollments.getContent().stream()
                .map(Enrollment::getUserId)
                .toList()
        );

        return enrollments.map(enrollment -> toResponse(enrollment, usersById.get(enrollment.getUserId())));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        UserLookupResponse user = lookupUsersById(List.of(enrollment.getUserId()))
            .get(enrollment.getUserId());

        return toResponse(enrollment, user);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, UserLookupResponse user) {
        Group group = enrollment.getGroup();

        return EnrollmentMapper.toResponse(enrollment, user, group.getSubject(), group);
    }

    private Map<Long, UserLookupResponse> lookupUsersById(List<Long> userIds) {
        return courseUserCacheService.lookupByIds(userIds);
    }

    private void requireEnrollableUser(Long userId, CourseRoleEnum role) {
        CourseUserCache user = courseUserCacheService.findActiveById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        String status = normalizeUserStatus(user.getStatus());
        if ("DROP_OUT".equals(status))
            throw new BadRequestException("Drop out user cannot be enrolled");
        if ("GRADUATED".equals(status) && role == CourseRoleEnum.LEARNER)
            throw new BadRequestException("Graduated user cannot be enrolled as learner");
    }

    private String normalizeUserStatus(String status) {
        return status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
    }

    private void rejectDuplicateEnrollment(Long userId, Long groupId) {
        if (enrollmentRepository.existsByUserIdAndGroupIdAndDeletedAtIsNull(userId, groupId))
            throw new ConflictException("User already enrolled in this group");
    }

    private void rejectSecondInstructor(Long groupId, CourseRoleEnum role) {
        if (role == CourseRoleEnum.INSTRUCTOR
            && enrollmentRepository.existsByGroupIdAndRoleAndDeletedAtIsNull(groupId, CourseRoleEnum.INSTRUCTOR))
            throw new ConflictException("Group already has an instructor");
    }

    private CourseRoleEnum normalizeRole(String role) {
        try { return CourseRoleEnum.from(role); } 
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid course role");
        }
    }

    private CourseRoleEnum normalizeSearchRole(String role) {
        return role == null || role.isBlank() ? null : normalizeRole(role);
    }

    private String normalizeSearchAcademicYear(String academicYear) {
        return academicYear == null || academicYear.isBlank() ? null : academicYear.trim();
    }

}
