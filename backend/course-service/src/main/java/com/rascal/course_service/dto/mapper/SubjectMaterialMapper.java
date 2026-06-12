package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.SubjectMaterialPatchRequest;
import com.rascal.course_service.dto.request.SubjectMaterialRequest;
import com.rascal.course_service.dto.response.SubjectMaterialResponse;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectMaterial;

public final class SubjectMaterialMapper {

    private SubjectMaterialMapper() { }

    public static SubjectMaterial toEntity(SubjectMaterialRequest request, Subject subject) {
        SubjectMaterial material = new SubjectMaterial();
        material.setSubject(subject);
        material.setMeetingNumber(request.meetingNumber());
        material.setTitle(request.title());
        material.setDescription(request.description());

        return material;
    }

    public static SubjectMaterialResponse toResponse(SubjectMaterial material) {
        Subject subject = material.getSubject();

        return new SubjectMaterialResponse(
            material.getId(),
            subject.getId(),
            subject.getName(),
            material.getMeetingNumber(),
            material.getTitle(),
            material.getDescription()
        );
    }

    public static void updateEntity(
        SubjectMaterial material,
        SubjectMaterialPatchRequest request,
        Subject subject
    ) {
        if (subject != null) material.setSubject(subject);
        if (request.meetingNumber() != null) material.setMeetingNumber(request.meetingNumber());
        if (request.title() != null) material.setTitle(request.title());
        if (request.description() != null) material.setDescription(request.description());

        material.setUpdatedAt(LocalDateTime.now());
    }
}
