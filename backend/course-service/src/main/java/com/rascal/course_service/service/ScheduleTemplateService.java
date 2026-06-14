package com.rascal.course_service.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.ScheduleTemplateMapper;
import com.rascal.course_service.dto.request.ScheduleTemplatePatchRequest;
import com.rascal.course_service.dto.request.ScheduleTemplateRequest;
import com.rascal.course_service.entity.ScheduleTemplate;
import com.rascal.course_service.repository.ScheduleTemplateRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class ScheduleTemplateService {

    private final ScheduleTemplateRepository scheduleTemplateRepository;

    public ScheduleTemplateService(ScheduleTemplateRepository scheduleTemplateRepository) {
        this.scheduleTemplateRepository = scheduleTemplateRepository;
    }

    @Transactional(readOnly = true)
    public Page<ScheduleTemplate> getAllPaged(Pageable pageable) {
        return scheduleTemplateRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public ScheduleTemplate getById(Long id) {
        return scheduleTemplateRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Schedule template not found"));
    }

    public ScheduleTemplate create(ScheduleTemplateRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        rejectDuplicateTimeRange(request.startTime(), request.endTime());

        ScheduleTemplate template = ScheduleTemplateMapper.toEntity(request);
        template.setCreatedAt(LocalDateTime.now());

        return scheduleTemplateRepository.save(template);
    }

    public ScheduleTemplate updateById(Long id, ScheduleTemplatePatchRequest request) {
        if (request.isEmptyPatch())
            throw new BadRequestException("Invalid patch");

        ScheduleTemplate template = getById(id);
        LocalTime startTime = request.startTime() == null ? template.getStartTime() : request.startTime();
        LocalTime endTime = request.endTime() == null ? template.getEndTime() : request.endTime();

        validateTimeRange(startTime, endTime);
        rejectDuplicateTimeRange(id, startTime, endTime);

        ScheduleTemplateMapper.updateEntity(template, request);

        return scheduleTemplateRepository.save(template);
    }

    public void deleteById(Long id) {
        ScheduleTemplate template = getById(id);
        template.setDeletedAt(LocalDateTime.now());

        scheduleTemplateRepository.save(template);
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime))
            throw new BadRequestException("Start time must be before end time");
    }

    private void rejectDuplicateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (scheduleTemplateRepository.existsByStartTimeAndEndTimeAndDeletedAtIsNull(startTime, endTime))
            throw new ConflictException("Schedule template time range already exists");
    }

    private void rejectDuplicateTimeRange(Long id, LocalTime startTime, LocalTime endTime) {
        if (scheduleTemplateRepository.existsByStartTimeAndEndTimeAndIdNotAndDeletedAtIsNull(startTime, endTime, id))
            throw new ConflictException("Schedule template time range already exists");
    }
}
