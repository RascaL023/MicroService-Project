package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.course_service.entity.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Page<Enrollment> findByDeletedAtIsNull(Pageable pageable);

    Optional<Enrollment> findByIdAndDeletedAtIsNull(Long id);

    Page<Enrollment> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<Enrollment> findBySubject_IdAndDeletedAtIsNull(Long subjectId, Pageable pageable);

    Page<Enrollment> findByGroup_IdAndDeletedAtIsNull(Long groupId, Pageable pageable);

    boolean existsByUserIdAndSubject_IdAndGroup_IdAndDeletedAtIsNull(Long userId, Long subjectId, Long groupId);
}
