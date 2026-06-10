package com.rascal.course_service.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.GroupSchedule;

public interface GroupScheduleRepository extends JpaRepository<GroupSchedule, Long> {

    @EntityGraph(attributePaths = {"group", "group.subject"})
    Optional<GroupSchedule> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select s
        from GroupSchedule s
        where s.deletedAt is null
            and (:groupId is null or s.group.id = :groupId)
            and (:dayOfWeek is null or s.dayOfWeek = :dayOfWeek)
    """)
    @EntityGraph(attributePaths = {"group", "group.subject"})
    Page<GroupSchedule> searchActiveSchedules(
        @Param("groupId") Long groupId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        Pageable pageable
    );

    @Query("""
        select count(s) > 0
        from GroupSchedule s
        where s.deletedAt is null
            and s.dayOfWeek = :dayOfWeek
            and s.startTime < :endTime
            and s.endTime > :startTime
            and (
                s.group.id = :groupId
                or s.group.subject.id <> :subjectId
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
        select count(s) > 0
        from GroupSchedule s
        where s.deletedAt is null
            and s.id <> :id
            and s.dayOfWeek = :dayOfWeek
            and s.startTime < :endTime
            and s.endTime > :startTime
            and (
                s.group.id = :groupId
                or s.group.subject.id <> :subjectId
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

}
