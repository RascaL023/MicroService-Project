package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.GroupSchedulePatchRequest;
import com.rascal.course_service.dto.request.GroupScheduleRequest;
import com.rascal.course_service.dto.response.GroupScheduleResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupSchedule;

public final class GroupScheduleMapper {

    private GroupScheduleMapper() { }

    public static GroupSchedule toEntity(GroupScheduleRequest request, Group group) {
        GroupSchedule schedule = new GroupSchedule();
        schedule.setGroup(group);
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());

        return schedule;
    }

    public static GroupScheduleResponse toResponse(GroupSchedule schedule) {
        Group group = schedule.getGroup();

        return new GroupScheduleResponse(
            schedule.getId(),
            group.getId(),
            group.getName(),
            group.getSubject().getId(),
            group.getSubject().getName(),
            schedule.getDayOfWeek().name(),
            schedule.getStartTime(),
            schedule.getEndTime()
        );
    }

    public static void updateEntity(GroupSchedule schedule, GroupSchedulePatchRequest request, Group group) {
        if (group != null) schedule.setGroup(group);
        if (request.startTime() != null) schedule.setStartTime(request.startTime());
        if (request.endTime() != null) schedule.setEndTime(request.endTime());

        schedule.setUpdatedAt(LocalDateTime.now());
    }
}
