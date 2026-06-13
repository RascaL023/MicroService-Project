package com.rascal.course_service.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.CourseUserCache;
import com.rascal.course_service.repository.CourseUserCacheRepository;

@Service
@Transactional
public class CourseUserCacheService {

    private final CourseUserCacheRepository courseUserCacheRepository;

    public CourseUserCacheService(CourseUserCacheRepository courseUserCacheRepository) {
        this.courseUserCacheRepository = courseUserCacheRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, UserLookupResponse> lookupByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        return courseUserCacheRepository.findByIdInAndDeletedAtIsNull(userIds)
            .stream()
            .map(this::toLookupResponse)
            .collect(Collectors.toMap(
                UserLookupResponse::id,
                Function.identity(),
                (left, right) -> left
            ));
    }

    @Transactional(readOnly = true)
    public boolean existsActive(Long userId) {
        return courseUserCacheRepository.existsByIdAndDeletedAtIsNull(userId);
    }

    public void upsert(UserLookupResponse user) {
        CourseUserCache cache = courseUserCacheRepository.findById(user.id())
            .orElseGet(CourseUserCache::new);

        cache.setId(user.id());
        cache.setName(user.name());
        cache.setGender(user.gender());
        cache.setBatch(user.batch());
        cache.setDeletedAt(null);
        cache.setSyncedAt(LocalDateTime.now());

        courseUserCacheRepository.save(cache);
    }

    public void softDelete(Long userId) {
        CourseUserCache cache = courseUserCacheRepository.findById(userId)
            .orElseGet(CourseUserCache::new);

        cache.setId(userId);
        cache.setDeletedAt(LocalDateTime.now());
        cache.setSyncedAt(LocalDateTime.now());

        courseUserCacheRepository.save(cache);
    }

    private UserLookupResponse toLookupResponse(CourseUserCache cache) {
        return new UserLookupResponse(
            cache.getId(),
            cache.getName(),
            cache.getGender(),
            cache.getBatch()
        );
    }

}
