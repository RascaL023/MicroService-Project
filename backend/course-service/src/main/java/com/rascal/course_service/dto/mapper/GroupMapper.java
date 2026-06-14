package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseStatusEnum;

public final class GroupMapper {

    private GroupMapper() { }

    public static Group toEntity(
        Group group, CourseStatusEnum status,
        String name, String academicYear, Subject subject
    ) {
        group.setStatus(status);
        group.setName(name);
        group.setAcademicYear(academicYear);
        group.setSubject(subject);

        return group;
    }

    public static GroupResponse toResponse(Group group, Subject subject) {
        return new GroupResponse(
            group.getId(),
            group.getName(),
            subject.getId(),
            subject.getName(),
            group.getAcademicYear(),
            group.getStatus().getDisplayName()
        );
    }

}
