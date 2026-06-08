package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.course_service.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Page<Group> findByDeletedAtIsNull(Pageable pageable);
    Optional<Group> findByIdAndDeletedAtIsNull(Long id);

    boolean existsBySubject_IdAndAcademicYearAndNameIgnoreCaseAndDeletedAtIsNull(
        Long subjectId,
        String academicYear,
        String name
    );

    boolean existsBySubject_IdAndAcademicYearAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
        Long subjectId,
        String academicYear,
        String name,
        Long id
    );

}
