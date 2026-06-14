package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.GroupSchedulePatchRequest;
import com.rascal.course_service.dto.request.GroupScheduleRequest;
import com.rascal.course_service.dto.response.GroupScheduleResponse;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupSchedule;
import com.rascal.course_service.entity.ScheduleTemplate;

public final class GroupScheduleMapper {

    private GroupScheduleMapper() { }

    public static GroupSchedule toEntity(GroupScheduleRequest request, Group group, ScheduleTemplate template) {
        GroupSchedule schedule = new GroupSchedule();
        schedule.setGroup(group);
        schedule.setScheduleTemplate(template);

        return schedule;
    }

    public static GroupScheduleResponse toResponse(GroupSchedule schedule) {
        Group group = schedule.getGroup();
        ScheduleTemplate template = schedule.getScheduleTemplate();

        return new GroupScheduleResponse(
            schedule.getId(),
            group.getId(),
            group.getName(),
            group.getSubject().getId(),
            group.getSubject().getName(),
            template == null ? null : template.getId(),
            template == null ? null : template.getName(),
            schedule.getDayOfWeek().name(),
            template == null ? null : template.getStartTime(),
            template == null ? null : template.getEndTime()
        );
    }

    public static void updateEntity(
        GroupSchedule schedule,
        GroupSchedulePatchRequest request,
        Group group,
        ScheduleTemplate template
    ) {
        if (group != null) schedule.setGroup(group);
        if (template != null) schedule.setScheduleTemplate(template);

        schedule.setUpdatedAt(LocalDateTime.now());
    }
}
