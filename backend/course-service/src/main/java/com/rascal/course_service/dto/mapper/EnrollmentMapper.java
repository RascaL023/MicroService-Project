package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.EnrollmentPatchRequest;
import com.rascal.course_service.dto.request.EnrollmentRequest;
import com.rascal.course_service.dto.response.EnrollmentResponse;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseRoleEnum;

public final class EnrollmentMapper {

    private EnrollmentMapper() { }

    public static Enrollment toEntity(EnrollmentRequest request, Subject subject, Group group) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(request.userId());
        enrollment.setRole(CourseRoleEnum.from(request.role()));
        enrollment.setSubject(subject);
        enrollment.setGroup(group);

        return enrollment;
    }

    public static EnrollmentResponse toResponse(Enrollment enrollment, String username) {
        return new EnrollmentResponse(
            enrollment.getId(),
            username,
            enrollment.getRole().getDisplayName(),
            enrollment.getSubject().getName(),
            enrollment.getGroup().getName()
        );
    }

    public static void updateEntity(Enrollment enrollment, EnrollmentPatchRequest request, Subject subject, Group group) {
        if (request.userId() != null) enrollment.setUserId(request.userId());
        if (request.role() != null) enrollment.setRole(CourseRoleEnum.from(request.role())); 
        if (subject != null) enrollment.setSubject(subject); 
        if (group != null) enrollment.setGroup(group);

        enrollment.setUpdatedAt(LocalDateTime.now());
    }
}
