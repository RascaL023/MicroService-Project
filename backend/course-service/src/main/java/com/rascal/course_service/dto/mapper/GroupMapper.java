package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.CourseStatusEnum;

public final class GroupMapper {

    private GroupMapper() { }

    public static Group toEntity(GroupRequest request, Subject subject) {
        Group group = new Group();
        group.setName(request.name());
        group.setAcademicYear(request.academicYear());
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

    public static void updateEntity(Group group, GroupPatchRequest request) {
        if (request.name() != null) group.setName(request.name());
        if (request.academicYear() != null) group.setAcademicYear(request.academicYear());
        if (request.isDone() != null) 
            group.setStatus(request.isDone() ? CourseStatusEnum.PASSED : CourseStatusEnum.ON_GOING);

        group.setUpdatedAt(LocalDateTime.now());
    }
}
