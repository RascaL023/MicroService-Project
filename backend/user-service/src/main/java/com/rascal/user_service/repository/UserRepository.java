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

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "batch")
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "batch")
    Page<User> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    @EntityGraph(attributePaths = "batch")
    List<User> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @EntityGraph(attributePaths = "batch")
    @Query("""
        select u
        from User u
        where u.deletedAt is null
            and (lower(u.name) like lower(concat('%', :name, '%')))
            and (:batchId is null or u.batch.id = :batchId)
    """)
    Page<User> searchActiveUsers(
        @Param("name") String name,
        @Param("batchId") Integer batchId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "batch")
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);
    long countByDeletedAtIsNull();
    long countByBatchIdAndDeletedAtIsNull(Integer batchId);

    @Query("""
        select u.batch.id as batchId, count(u.id) as userCount
        from User u
        where u.deletedAt is null
            and u.batch.id in :batchIds
        group by u.batch.id
    """)
    List<BatchUserCount> countActiveUsersByBatchIds(@Param("batchIds") Collection<Integer> batchIds);
    
}
