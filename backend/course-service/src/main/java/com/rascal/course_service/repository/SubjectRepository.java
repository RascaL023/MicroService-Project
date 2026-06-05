package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.course_service.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByDeletedAtIsNull(Pageable pageable);

    Optional<Subject> findByIdAndDeletedAtIsNull(Long id);
}
