package com.rascal.course_service.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.GroupSchedule;

public interface GroupScheduleRepository extends JpaRepository<GroupSchedule, Long> {

    @EntityGraph(attributePaths = {"group", "group.subject", "scheduleTemplate"})
    Optional<GroupSchedule> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT s
        FROM GroupSchedule s
        WHERE s.deletedAt IS NULL
            AND s.group.id = :groupId
        ORDER BY s.dayOfWeek ASC, s.scheduleTemplate.startTime ASC
    """)
    @EntityGraph(attributePaths = {"group", "group.subject", "scheduleTemplate"})
    List<GroupSchedule> findActiveByGroupIdOrderByDayAndTemplateStartTime(@Param("groupId") Long groupId);

    @Query("""
        SELECT s
        FROM GroupSchedule s
        WHERE s.deletedAt IS NULL
            AND (:groupId IS NULL OR s.group.id = :groupId)
            AND (:dayOfWeek IS NULL OR s.dayOfWeek = :dayOfWeek)
    """)
    @EntityGraph(attributePaths = {"group", "group.subject", "scheduleTemplate"})
    Page<GroupSchedule> searchActiveSchedules(
        @Param("groupId") Long groupId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(s) > 0
        FROM GroupSchedule s
        WHERE s.deletedAt IS NULL
            AND s.dayOfWeek = :dayOfWeek
            AND s.scheduleTemplate.startTime < :endTime
            AND s.scheduleTemplate.endTime > :startTime
            AND (
                s.group.id = :groupId
                OR s.group.subject.id <> :subjectId
            )
    """)
    boolean existsActiveOverlap(
        @Param("groupId") Long groupId,
        @Param("subjectId") Long subjectId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Query("""
        SELECT COUNT(s) > 0
        FROM GroupSchedule s
        WHERE s.deletedAt IS NULL
            AND s.id <> :id
            AND s.dayOfWeek = :dayOfWeek
            AND s.scheduleTemplate.startTime < :endTime
            AND s.scheduleTemplate.endTime > :startTime
            AND (
                s.group.id = :groupId
                OR s.group.subject.id <> :subjectId
            )
    """)
    boolean existsActiveOverlapExcludingId(
        @Param("id") Long id,
        @Param("groupId") Long groupId,
        @Param("subjectId") Long subjectId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Modifying
    @Query("""
        DELETE FROM GroupSchedule s
        WHERE s.group.id in :groupIds
    """)
    int deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

}
