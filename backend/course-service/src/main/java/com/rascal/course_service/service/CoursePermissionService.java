package com.rascal.course_service.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.rascal.course_service.dto.response.GroupAccessResponse;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.enumerated.CourseRoleEnum;
import com.rascal.course_service.repository.EnrollmentRepository;

@Service
public class CoursePermissionService {

    private final CurrentUserService currentUserService;
    private final EnrollmentRepository enrollmentRepository;

    public CoursePermissionService(
        CurrentUserService currentUserService,
        EnrollmentRepository enrollmentRepository
    ) {
        this.currentUserService = currentUserService;
        this.enrollmentRepository = enrollmentRepository;
    }


    public GroupAccessResponse getAccessByGroupId(Long groupId) {
        Long userId = currentUserService.getUserId();
        boolean hasAccessGlobally = currentUserService.hasAnyAuthority(
            "course.*", "group.*", "enrollment.*"
        );

        String role = null;
        boolean isMember = false;
        boolean isInstructor = false;
        boolean canManage = hasAccessGlobally;
        boolean canRead = canManage;

        if (!canManage) {
            Enrollment enrollment = enrollmentRepository
                .findByUserIdAndGroupIdAndDeletedAtIsNull(userId, groupId)
                .orElse(null);

            role = enrollment == null ? null : enrollment.getRole().name();
            isMember = enrollment != null;
            isInstructor = enrollment != null && enrollment.getRole() == CourseRoleEnum.INSTRUCTOR;
            canRead = isMember;
            canManage = isInstructor;
        }

        return new GroupAccessResponse(
            userId, groupId,
            role,
            isMember,
            isInstructor,
            hasAccessGlobally,
            canRead,
            canManage
        );
    }

    public void requireGroupMember(Long groupId) {
        GroupAccessResponse access = getAccessByGroupId(groupId);
        if (!access.canRead()) throw new AccessDeniedException("Forbidden");
    }

    public void requireGroupInstructor(Long groupId) {
        GroupAccessResponse access = getAccessByGroupId(groupId);
        if (!access.canManage()) throw new AccessDeniedException("Forbidden");
    }
    
}
