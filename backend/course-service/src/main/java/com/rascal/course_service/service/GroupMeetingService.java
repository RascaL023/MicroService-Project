package com.rascal.course_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.GroupMeetingMapper;
import com.rascal.course_service.dto.request.GroupMeetingDoneRequest;
import com.rascal.course_service.dto.request.GroupMeetingStartRequest;
import com.rascal.course_service.dto.response.GroupMeetingResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupMeeting;
import com.rascal.course_service.entity.SubjectMaterial;
import com.rascal.course_service.enumerated.CourseStatusEnum;
import com.rascal.course_service.enumerated.GroupMeetingStatusEnum;
import com.rascal.course_service.repository.GroupMeetingRepository;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.SubjectMaterialRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class GroupMeetingService {

    private final GroupMeetingRepository groupMeetingRepository;
    private final GroupRepository groupRepository;
    private final SubjectMaterialRepository subjectMaterialRepository;
    private final CoursePermissionService coursePermissionService;

    public GroupMeetingService(
        GroupMeetingRepository groupMeetingRepository,
        GroupRepository groupRepository,
        SubjectMaterialRepository subjectMaterialRepository,
        CoursePermissionService coursePermissionService
    ) {
        this.groupMeetingRepository = groupMeetingRepository;
        this.groupRepository = groupRepository;
        this.subjectMaterialRepository = subjectMaterialRepository;
        this.coursePermissionService = coursePermissionService;
    }

    @Transactional(readOnly = true)
    public Page<GroupMeetingResponse> getAllPaged(
        Long groupId,
        Long subjectId,
        String status,
        Pageable pageable
    ) {
        return groupMeetingRepository
            .searchActiveMeetings(groupId, subjectId, normalizeSearchStatus(status), pageable)
            .map(GroupMeetingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public GroupMeetingResponse getByIdResponse(Long id) {
        return GroupMeetingMapper.toResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public List<GroupMeetingResponse> getTimelineByGroup(Group group) {
        List<SubjectMaterial> materials = subjectMaterialRepository
            .findActiveBySubjectIdOrderByMeetingNumberAsc(group.getSubject().getId());
        Map<Long, GroupMeeting> meetingsByMaterialId = groupMeetingRepository
            .findByGroupIdAndDeletedAtIsNullOrderBySubjectMaterialMeetingNumberAsc(group.getId())
            .stream()
            .collect(Collectors.toMap(
                meeting -> meeting.getSubjectMaterial().getId(),
                Function.identity(),
                (first, ignored) -> first
            ));

        return materials.stream()
            .map(material -> {
                GroupMeeting meeting = meetingsByMaterialId.get(material.getId());
                return meeting == null ?
                    GroupMeetingMapper.toNotStartedResponse(group, material) :
                    GroupMeetingMapper.toResponse(meeting);
            })
            .toList();
    }

    public GroupMeetingResponse start(GroupMeetingStartRequest request) {
        Group group = getActiveGroup(request.groupId());
        coursePermissionService.requireGroupInstructor(group.getId());
        rejectInactiveGroup(group);

        SubjectMaterial material = getActiveMaterial(request.subjectMaterialId());
        rejectMaterialFromDifferentSubject(group, material);
        rejectAlreadyStarted(group.getId(), material.getId());

        LocalDateTime now = LocalDateTime.now();
        GroupMeeting meeting = new GroupMeeting();
        meeting.setGroup(group);
        meeting.setSubjectMaterial(material);
        meeting.setMeetingDate(request.meetingDate() == null ? LocalDate.now() : request.meetingDate());
        meeting.setStatus(GroupMeetingStatusEnum.STARTED);
        meeting.setNote(normalizeNote(request.note()));
        meeting.setStartedAt(now);
        meeting.setCreatedAt(now);

        return GroupMeetingMapper.toResponse(groupMeetingRepository.save(meeting));
    }

    public GroupMeetingResponse markDone(Long id, GroupMeetingDoneRequest request) {
        GroupMeeting meeting = getById(id);
        coursePermissionService.requireGroupInstructor(meeting.getGroup().getId());
        rejectInactiveGroup(meeting.getGroup());
        if (meeting.getStatus() == GroupMeetingStatusEnum.DONE)
            throw new ConflictException("Meeting already done");

        LocalDateTime now = LocalDateTime.now();
        String note = request == null ? null : request.note();
        if (note != null) meeting.setNote(normalizeNote(note));
        meeting.setStatus(GroupMeetingStatusEnum.DONE);
        meeting.setCompletedAt(now);
        meeting.setUpdatedAt(now);

        return GroupMeetingMapper.toResponse(groupMeetingRepository.save(meeting));
    }

    private GroupMeeting getById(Long id) {
        return groupMeetingRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Group meeting not found"));
    }

    private Group getActiveGroup(Long groupId) {
        return groupRepository.findByIdAndDeletedAtIsNull(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    private SubjectMaterial getActiveMaterial(Long subjectMaterialId) {
        return subjectMaterialRepository.findByIdAndDeletedAtIsNull(subjectMaterialId)
            .orElseThrow(() -> new NotFoundException("Subject material not found"));
    }

    private void rejectMaterialFromDifferentSubject(Group group, SubjectMaterial material) {
        if (!group.getSubject().getId().equals(material.getSubject().getId()))
            throw new BadRequestException("Subject material does not belong to group subject");
    }

    private void rejectInactiveGroup(Group group) {
        if (group.getStatus() != CourseStatusEnum.ON_GOING)
            throw new BadRequestException("Group is not active");
    }

    private void rejectAlreadyStarted(Long groupId, Long subjectMaterialId) {
        if (groupMeetingRepository.existsByGroupIdAndSubjectMaterialIdAndDeletedAtIsNull(groupId, subjectMaterialId))
            throw new ConflictException("Meeting already started");
    }

    private GroupMeetingStatusEnum normalizeSearchStatus(String status) {
        if (status == null || status.isBlank()) return null;

        try { return GroupMeetingStatusEnum.from(status); }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid meeting status");
        }
    }

    private String normalizeNote(String note) {
        if (note == null) return null;

        String normalized = note.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
