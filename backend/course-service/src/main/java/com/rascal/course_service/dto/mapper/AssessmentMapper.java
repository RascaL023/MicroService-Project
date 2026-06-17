package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.AssessmentResponse;
import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupMeeting;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectMaterial;

public final class AssessmentMapper {

    private AssessmentMapper() { }

    public static AssessmentResponse toResponse(Assessment assessment) {
        Group group = assessment.getGroup();
        Subject subject = group.getSubject();
        GroupMeeting meeting = assessment.getGroupMeeting();
        SubjectMaterial material = meeting == null ? null : meeting.getSubjectMaterial();

        return new AssessmentResponse(
            assessment.getId(),
            group.getId(),
            group.getName(),
            subject.getId(),
            subject.getName(),
            meeting == null ? null : meeting.getId(),
            material == null ? null : material.getId(),
            material == null ? null : material.getMeetingNumber(),
            material == null ? null : material.getTitle(),
            assessment.getType().name(),
            assessment.getType().getDisplayName(),
            assessment.getTitle(),
            assessment.getDescription(),
            assessment.getType().getWeight(),
            assessment.getDueAt(),
            assessment.getStoredFilename() != null,
            assessment.getOriginalFilename(),
            assessment.getFilePath(),
            assessment.getMimeType(),
            assessment.getFileSize()
        );
    }
}
