package com.rascal.course_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByDeletedAtIsNull(Pageable pageable);
    Optional<Subject> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select s
        from Subject s
        where s.deletedAt is null
            and (:name is null or lower(s.name) like lower(concat('%', :name, '%')))
    """)
    Page<Subject> searchActiveSubjects(
        @Param("name") String name,
        Pageable pageable
    );

}
