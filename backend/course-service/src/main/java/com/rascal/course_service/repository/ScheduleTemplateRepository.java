package com.rascal.course_service.repository;

import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.course_service.entity.ScheduleTemplate;

public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long> {

    Optional<ScheduleTemplate> findByIdAndDeletedAtIsNull(Long id);

    Page<ScheduleTemplate> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByStartTimeAndEndTimeAndDeletedAtIsNull(LocalTime startTime, LocalTime endTime);

    boolean existsByStartTimeAndEndTimeAndIdNotAndDeletedAtIsNull(
        LocalTime startTime,
        LocalTime endTime,
        Long id
    );
}
