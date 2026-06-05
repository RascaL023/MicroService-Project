package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.SubjectPatchRequest;
import com.rascal.course_service.dto.request.SubjectRequest;
import com.rascal.course_service.dto.response.SubjectResponse;
import com.rascal.course_service.entity.Subject;

public final class SubjectMapper {

    private SubjectMapper() { }

    public static Subject toEntity(SubjectRequest request) {
        Subject subject = new Subject();
        subject.setName(request.name());

        return subject;
    }

    public static SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
            subject.getId(),
            subject.getName()
        );
    }

    public static void updateEntity(Subject subject, SubjectPatchRequest request) {
        if (request.name() != null) subject.setName(request.name());

        subject.setUpdatedAt(LocalDateTime.now());
    }
}
