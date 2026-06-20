package com.rascal.course_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.course_service.entity.CourseUserCache;

public interface CourseUserCacheRepository extends JpaRepository<CourseUserCache, Long> {

    // List<CourseUserCache> findByIdInAndDeletedAtIsNull(Collection<Long> ids);
    List<CourseUserCache> findByIdIn(Collection<Long> ids);
    boolean existsByIdAndDeletedAtIsNull(Long id);
    Optional<CourseUserCache> findByIdAndDeletedAtIsNull(Long id);

}
