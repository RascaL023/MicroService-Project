package com.rascal.course_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.enumerated.CourseRoleEnum;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = {"group", "group.subject"})
    Page<Enrollment> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"group", "group.subject"})
    Optional<Enrollment> findByIdAndDeletedAtIsNull(Long id);
    Optional<Enrollment> findByUserIdAndGroupIdAndDeletedAtIsNull(Long userId, Long groupId);
    Optional<Enrollment> findByUserIdAndGroupIdAndRoleAndDeletedAtIsNull(Long userId, Long groupId, CourseRoleEnum role);

    @EntityGraph(attributePaths = {"group", "group.subject"})
    List<Enrollment> findByGroupIdAndDeletedAtIsNullOrderByRoleAscUserIdAsc(Long groupId);
    List<Enrollment> findByGroupIdAndRoleAndDeletedAtIsNull(Long groupId, CourseRoleEnum role);

    boolean existsByUserIdAndGroupIdAndDeletedAtIsNull(Long userId, Long groupId);
    boolean existsByUserIdAndGroupIdAndRoleAndDeletedAtIsNull(Long userId, Long groupId, CourseRoleEnum role);
    boolean existsByGroupIdAndRoleAndDeletedAtIsNull(Long groupId, CourseRoleEnum role);

    @Query("""
        select e
        from Enrollment e
        where e.deletedAt is null
            and (:userId is null or e.userId = :userId)
            and (:groupId is null or e.group.id = :groupId)
            and (:subjectId is null or e.group.subject.id = :subjectId)
            and (:academicYear is null or e.group.academicYear = :academicYear)
            and (:role is null or e.role = :role)
    """)
    @EntityGraph(attributePaths = {"group", "group.subject"})
    Page<Enrollment> searchActiveEnrollments(
        @Param("userId") Long userId,
        @Param("groupId") Long groupId,
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("role") CourseRoleEnum role,
        Pageable pageable
    );

}
