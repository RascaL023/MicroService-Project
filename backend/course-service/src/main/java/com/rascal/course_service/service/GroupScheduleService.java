package com.rascal.course_service.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.GroupScheduleMapper;
import com.rascal.course_service.dto.request.GroupSchedulePatchRequest;
import com.rascal.course_service.dto.request.GroupScheduleRequest;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupSchedule;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.GroupScheduleRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class GroupScheduleService {

    private final GroupScheduleRepository groupScheduleRepository;
    private final GroupRepository groupRepository;

    public GroupScheduleService(
        GroupScheduleRepository groupScheduleRepository,
        GroupRepository groupRepository
    ) {
        this.groupScheduleRepository = groupScheduleRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public Page<GroupSchedule> getAllPaged(Long groupId, String dayOfWeek, Pageable pageable) {
        return groupScheduleRepository.searchActiveSchedules(
            groupId,
            normalizeSearchDayOfWeek(dayOfWeek),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public GroupSchedule getById(Long id) {
        return groupScheduleRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Group schedule not found"));
    }

    public GroupSchedule create(GroupScheduleRequest request) {
        Group group = getActiveGroup(request.groupId());
        DayOfWeek dayOfWeek = normalizeDayOfWeek(request.dayOfWeek());
        validateTimeRange(request.startTime(), request.endTime());

        rejectOverlappingSchedule(
            group.getId(),
            group.getSubject().getId(),
            dayOfWeek,
            request.startTime(),
            request.endTime()
        );

        GroupSchedule schedule = GroupScheduleMapper.toEntity(request, group);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setCreatedAt(LocalDateTime.now());

        return groupScheduleRepository.save(schedule);
    }

    public GroupSchedule updateById(Long id, GroupSchedulePatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        GroupSchedule schedule = getById(id);
        Group group = request.groupId() == null ? schedule.getGroup() : getActiveGroup(request.groupId());
        DayOfWeek dayOfWeek = request.dayOfWeek() == null ?
            schedule.getDayOfWeek() : normalizeDayOfWeek(request.dayOfWeek());
        LocalTime startTime = request.startTime() == null ? schedule.getStartTime() : request.startTime();
        LocalTime endTime = request.endTime() == null ? schedule.getEndTime() : request.endTime();

        validateTimeRange(startTime, endTime);
        rejectOverlappingSchedule(
            id,
            group.getId(),
            group.getSubject().getId(),
            dayOfWeek,
            startTime,
            endTime
        );

        schedule.setDayOfWeek(dayOfWeek);
        GroupScheduleMapper.updateEntity(schedule, request, group);

        return groupScheduleRepository.save(schedule);
    }

    public void deleteById(Long id) {
        GroupSchedule schedule = getById(id);
        schedule.setDeletedAt(LocalDateTime.now());

        groupScheduleRepository.save(schedule);
    }

    private Group getActiveGroup(Long groupId) {
        return groupRepository.findByIdAndDeletedAtIsNull(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime))
            throw new BadRequestException("Start time must be before end time");
    }

    private void rejectOverlappingSchedule(
        Long groupId,
        Long subjectId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {
        if (groupScheduleRepository.existsActiveOverlap(groupId, subjectId, dayOfWeek, startTime, endTime))
            throw new ConflictException("Group schedule overlaps with existing schedule");
    }

    private void rejectOverlappingSchedule(
        Long id,
        Long groupId,
        Long subjectId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {
        if (groupScheduleRepository.existsActiveOverlapExcludingId(id, groupId, subjectId, dayOfWeek, startTime, endTime))
            throw new ConflictException("Group schedule overlaps with existing schedule");
    }

    private DayOfWeek normalizeSearchDayOfWeek(String dayOfWeek) {
        return dayOfWeek == null || dayOfWeek.isBlank() ? null : normalizeDayOfWeek(dayOfWeek);
    }

    private DayOfWeek normalizeDayOfWeek(String dayOfWeek) {
        try { return DayOfWeek.valueOf(dayOfWeek.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid day of week");
        }
    }
}
