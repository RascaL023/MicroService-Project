package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

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
        """)
    @EntityGraph(attributePaths = "subject")
    Page<Group> searchActiveGroups(
        @Param("name") String name,
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        Pageable pageable
    );

}
