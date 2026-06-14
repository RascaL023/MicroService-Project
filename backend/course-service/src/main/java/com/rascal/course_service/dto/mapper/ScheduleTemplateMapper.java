package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.ScheduleTemplatePatchRequest;
import com.rascal.course_service.dto.request.ScheduleTemplateRequest;
import com.rascal.course_service.dto.response.ScheduleTemplateResponse;
import com.rascal.course_service.entity.ScheduleTemplate;

public final class ScheduleTemplateMapper {

    private ScheduleTemplateMapper() { }

    public static ScheduleTemplate toEntity(ScheduleTemplateRequest request) {
        ScheduleTemplate template = new ScheduleTemplate();
        template.setName(request.name().trim());
        template.setStartTime(request.startTime());
        template.setEndTime(request.endTime());

        return template;
    }

    public static ScheduleTemplateResponse toResponse(ScheduleTemplate template) {
        return new ScheduleTemplateResponse(
            template.getId(),
            template.getName(),
            template.getStartTime(),
            template.getEndTime()
        );
    }

    public static void updateEntity(ScheduleTemplate template, ScheduleTemplatePatchRequest request) {
        if (request.name() != null) template.setName(request.name().trim());
        if (request.startTime() != null) template.setStartTime(request.startTime());
        if (request.endTime() != null) template.setEndTime(request.endTime());

        template.setUpdatedAt(LocalDateTime.now());
    }
}
