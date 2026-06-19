package com.rascal.course_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.Group;
import com.rascal.course_service.enumerated.CourseStatusEnum;
import com.rascal.course_service.repository.projection.CourseDashboardSummary;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query(value = """
        SELECT
            COUNT(*) FILTER (
                WHERE g.deleted_at IS NULL AND g.status = 'ON_GOING'
            ) AS "activeGroups",
            (
                SELECT COUNT(*)
                FROM subjects s
                WHERE s.deleted_at IS NULL
            ) AS "subjects",
            (
                SELECT COUNT(DISTINCT e.user_id)
                FROM enrollments e
                JOIN groups eg ON eg.id = e.group_id
                WHERE e.deleted_at IS NULL
                    AND eg.deleted_at IS NULL
                    AND eg.status = 'ON_GOING'
                    AND e.role = 'INSTRUCTOR'
            ) AS "instructors",
            COUNT(*) FILTER (
                WHERE g.deleted_at IS NULL
                    AND g.status = 'ON_GOING'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM enrollments e
                        WHERE e.group_id = g.id
                            AND e.deleted_at IS NULL
                            AND e.role = 'INSTRUCTOR'
                    )
            ) AS "groupsWithoutInstructor",
            COUNT(*) FILTER (
                WHERE g.deleted_at IS NULL
                    AND g.status = 'ON_GOING'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM group_schedules gs
                        WHERE gs.group_id = g.id
                            AND gs.deleted_at IS NULL
                    )
            ) AS "groupsWithoutSchedule"
        FROM groups g
        """, nativeQuery = true)
    CourseDashboardSummary getDashboardSummary();

    @EntityGraph(attributePaths = "subject")
    Optional<Group> findByIdAndDeletedAtIsNull(Long id);

    boolean existsBySubjectIdAndAcademicYearAndNameIgnoreCaseAndDeletedAtIsNull(
        Long subjectId,
        String academicYear,
        String name
    );

    boolean existsBySubjectIdAndAcademicYearAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
        Long subjectId,
        String academicYear,
        String name,
        Long id
    );

    @Query("""
        SELECT g
        FROM Group g
        WHERE g.deletedAt IS NULL
        AND (LOWER(g.name) LIKE LOWER(concat('%', CAST(:name AS STRING), '%')))
        AND (:subjectId IS NULL OR g.subject.id = :subjectId)
        AND (:academicYear IS NULL OR g.academicYear = :academicYear)
        AND (:status IS NULL OR g.status = :status)
        """)
    @EntityGraph(attributePaths = "subject")
    Page<Group> searchActiveGroups(
        @Param("name") String name,
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("status") CourseStatusEnum status,
        Pageable pageable
    );

    @Query("""
        SELECT g.id
        FROM Group g
        WHERE g.deletedAt IS NULL
            AND g.subject.id = :subjectId
            AND g.academicYear = :academicYear
            AND g.status = :status
    """)
    List<Long> findActiveIdsBySubjectIdAndAcademicYearAndStatus(
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("status") CourseStatusEnum status
    );

    default List<Long> findActiveOngoingIdsBySubjectIdAndAcademicYear(
        Long subjectId,
        String academicYear
    ) {
        return findActiveIdsBySubjectIdAndAcademicYearAndStatus(
            subjectId,
            academicYear,
            CourseStatusEnum.ON_GOING
        );
    }

    @Modifying
    @Query("""
        UPDATE Group g
        SET g.status = :status,
            g.updatedAt = :updatedAt
        WHERE g.id in :ids
            AND g.deletedAt IS NULL
    """)
    int markStatusByIds(
        @Param("ids") List<Long> ids,
        @Param("status") CourseStatusEnum status,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    default int markPassedByIds(List<Long> ids, LocalDateTime updatedAt) {
        return markStatusByIds(ids, CourseStatusEnum.PASSED, updatedAt);
    }

}
