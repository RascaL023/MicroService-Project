package com.rascal.user_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.user_service.entity.Major;

public interface MajorRepository extends JpaRepository<Major, String> {
    Page<Major> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Major> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);
    @Query("""
        SELECT m
        FROM Major m
        WHERE m.deletedAt IS NULL
            AND (
                LOWER(m.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
    """)
    Page<Major> searchActiveMajors(@Param("keyword") String keyword, Pageable pageable);
    List<Major> findByIdInAndDeletedAtIsNull(Collection<String> ids);
    Optional<Major> findByIdAndDeletedAtIsNull(String id);
    boolean existsByIdAndDeletedAtIsNull(String id);
    long countByDeletedAtIsNull();
}
