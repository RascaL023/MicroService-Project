package com.rascal.user_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.user_service.entity.User;
import com.rascal.user_service.repository.projection.BatchUserCount;
import com.rascal.user_service.repository.projection.MajorUserCount;
import com.rascal.user_service.repository.projection.UserDashboardSummaryProjection;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"batch", "major"})
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"batch", "major"})
    Page<User> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"batch", "major"})
    List<User> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @EntityGraph(attributePaths = {"batch", "major"})
    @Query("""
        SELECT u
        FROM User u
        WHERE u.deletedAt IS NULL
            AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:batchId IS NULL OR u.batch.id = :batchId)
            AND (:majorId IS NULL OR u.major.id = :majorId)
    """)
    Page<User> searchActiveUsers(
        @Param("name") String name,
        @Param("batchId") Integer batchId,
        @Param("majorId") String majorId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"batch", "major"})
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);
    long countByDeletedAtIsNull();
    long countByBatchIdAndDeletedAtIsNull(Integer batchId);
    long countByMajorIdAndDeletedAtIsNull(String majorId);

    @Query(value = """
        SELECT
            (SELECT COUNT(*) FROM users u WHERE u.deleted_at IS NULL) AS "totalUsers",
            (SELECT COUNT(*) FROM batches b WHERE b.deleted_at IS NULL) AS "totalBatches",
            (SELECT COUNT(*) FROM majors m WHERE m.deleted_at IS NULL) AS "totalMajors"
        """, nativeQuery = true)
    UserDashboardSummaryProjection getDashboardSummary();

    @Query("""
        SELECT u.batch.id AS batchId, COUNT(u.id) AS userCount
        FROM User u
        WHERE u.deletedAt IS NULL
            AND u.batch.id IN :batchIds
        GROUP BY u.batch.id
    """)
    List<BatchUserCount> countActiveUsersByBatchIds(@Param("batchIds") Collection<Integer> batchIds);

    @Query("""
        SELECT u.major.id AS majorId, COUNT(u.id) AS userCount
        FROM User u
        WHERE u.deletedAt IS NULL
            AND u.major.id IN :majorIds
        GROUP BY u.major.id
    """)
    List<MajorUserCount> countActiveUsersByMajorIds(@Param("majorIds") Collection<String> majorIds);
    
}
