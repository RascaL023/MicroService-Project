package com.rascal.course_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.enumerated.AssessmentTypeEnum;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @EntityGraph(attributePaths = {"group", "group.subject", "groupMeeting", "groupMeeting.subjectMaterial"})
    Optional<Assessment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select a
        from Assessment a
        where a.deletedAt is null
            and a.group.id = :groupId
        order by a.dueAt asc nulls last, a.createdAt desc
    """)
    @EntityGraph(attributePaths = {"group", "group.subject", "groupMeeting", "groupMeeting.subjectMaterial"})
    List<Assessment> findActiveByGroupIdOrderByDueAt(@Param("groupId") Long groupId);

    @Query("""
        select a
        from Assessment a
        where a.deletedAt is null
            and (:groupId is null or a.group.id = :groupId)
            and (:groupMeetingId is null or a.groupMeeting.id = :groupMeetingId)
            and (:subjectId is null or a.group.subject.id = :subjectId)
            and (:type is null or a.type = :type)
            and lower(a.title) like lower(concat('%', cast(:title as string), '%'))
    """)
    @EntityGraph(attributePaths = {"group", "group.subject", "groupMeeting", "groupMeeting.subjectMaterial"})
    Page<Assessment> searchActiveAssessments(
        @Param("groupId") Long groupId,
        @Param("groupMeetingId") Long groupMeetingId,
        @Param("subjectId") Long subjectId,
        @Param("type") AssessmentTypeEnum type,
        @Param("title") String title,
        Pageable pageable
    );

}
