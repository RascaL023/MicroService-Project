package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.GroupMeetingResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupMeeting;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectMaterial;

public final class GroupMeetingMapper {

    private GroupMeetingMapper() { }

    public static GroupMeetingResponse toResponse(GroupMeeting meeting) {
        Group group = meeting.getGroup();
        SubjectMaterial material = meeting.getSubjectMaterial();
        Subject subject = material.getSubject();

        return new GroupMeetingResponse(
            meeting.getId(),
            group.getId(),
            group.getName(),
            subject.getId(),
            subject.getName(),
            material.getId(),
            material.getMeetingNumber(),
            material.getTitle(),
            material.getDescription(),
            meeting.getMeetingDate(),
            meeting.getStatus().name(),
            meeting.getStatus().getDisplayName(),
            meeting.getNote(),
            meeting.getStartedAt(),
            meeting.getCompletedAt()
        );
    }

    public static GroupMeetingResponse toNotStartedResponse(Group group, SubjectMaterial material) {
        Subject subject = material.getSubject();

        return new GroupMeetingResponse(
            null,
            group.getId(),
            group.getName(),
            subject.getId(),
            subject.getName(),
            material.getId(),
            material.getMeetingNumber(),
            material.getTitle(),
            material.getDescription(),
            null,
            "NOT_STARTED",
            "Belum dimulai",
            null,
            null,
            null
        );
    }
}
