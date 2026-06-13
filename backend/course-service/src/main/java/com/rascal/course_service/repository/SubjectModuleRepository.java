package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.SubjectModule;

public interface SubjectModuleRepository extends JpaRepository<SubjectModule, Long> {

    @EntityGraph(attributePaths = "subject")
    Optional<SubjectModule> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select m
        from SubjectModule m
        where m.deletedAt is null
            and (:subjectId is null or m.subject.id = :subjectId)
            and lower(m.originalFilename) like lower(concat('%', cast(:filename as string), '%'))
    """)
    @EntityGraph(attributePaths = "subject")
    Page<SubjectModule> searchActiveModules(
        @Param("subjectId") Long subjectId,
        @Param("filename") String filename,
        Pageable pageable
    );

}
