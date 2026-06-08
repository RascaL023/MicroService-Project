package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.request.EnrollmentPatchRequest;
import com.rascal.course_service.dto.request.EnrollmentRequest;
import com.rascal.course_service.dto.response.EnrollmentResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseRoleEnum;

public class EnrollmentMapper {

    public static Enrollment toEntity(EnrollmentRequest request, Group group) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(request.userId());
        enrollment.setGroup(group);
        enrollment.setRole(CourseRoleEnum.from(request.role()));

        return enrollment;
    }

    public static EnrollmentResponse toResponse(
        Enrollment enrollment, 
        UserLookupResponse user,
        Subject subject, Group group
    ) {
        UserLookupResponse resolvedUser = user == null
            ? new UserLookupResponse(enrollment.getUserId(), null, null, null)
            : user;

        return new EnrollmentResponse(
            enrollment.getId(), 
            resolvedUser,
            enrollment.getRole().getDisplayName(), 
            subject.getId(), 
            subject.getName(), 
            group.getId(), 
            group.getName(), 
            group.getAcademicYear()
        );
    }

    public static void updateEntity(
        Enrollment enrollment, EnrollmentPatchRequest request,
        Group group
    ) {
        if (request.userId() != null) enrollment.setUserId(request.userId());
        if (request.role() != null) enrollment.setRole(CourseRoleEnum.from(request.role()));
        if (group != null) enrollment.setGroup(group);
    }
    
}
