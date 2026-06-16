package com.rascal.course_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.SubjectMaterial;

public interface SubjectMaterialRepository extends JpaRepository<SubjectMaterial, Long> {

    @EntityGraph(attributePaths = "subject")
    Optional<SubjectMaterial> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT m
        FROM SubjectMaterial m
        WHERE m.deletedAt IS NULL
            AND m.subject.id = :subjectId
        ORDER BY m.meetingNumber ASC
    """)
    @EntityGraph(attributePaths = "subject")
    List<SubjectMaterial> findActiveBySubjectIdOrderByMeetingNumberAsc(@Param("subjectId") Long subjectId);

    @Query("""
        SELECT m
        FROM SubjectMaterial m
        WHERE m.deletedAt IS NULL
            AND (:subjectId IS NULL OR m.subject.id = :subjectId)
            AND lower(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))
    """)
    @EntityGraph(attributePaths = "subject")
    Page<SubjectMaterial> searchActiveMaterials(
        @Param("subjectId") Long subjectId,
        @Param("title") String title,
        Pageable pageable
    );

    boolean existsBySubject_IdAndMeetingNumberAndDeletedAtIsNull(
        Long subjectId,
        Integer meetingNumber
    );

    boolean existsBySubject_IdAndMeetingNumberAndIdNotAndDeletedAtIsNull(
        Long subjectId,
        Integer meetingNumber,
        Long id
    );
}
