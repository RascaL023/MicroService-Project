package com.rascal.course_service.repository;

import java.util.Optional;

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
        select m
        from SubjectMaterial m
        where m.deletedAt is null
            and (:subjectId is null or m.subject.id = :subjectId)
            and lower(m.title) like lower(concat('%', cast(:title as string), '%'))
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
