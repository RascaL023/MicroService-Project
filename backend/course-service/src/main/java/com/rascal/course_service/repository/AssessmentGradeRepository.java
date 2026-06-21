package com.rascal.course_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.AssessmentGrade;

public interface AssessmentGradeRepository extends JpaRepository<AssessmentGrade, Long> {

    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    Optional<AssessmentGrade> findByAssessmentIdAndUserId(Long assessmentId, Long userId);

    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    List<AssessmentGrade> findByAssessmentIdOrderByUserIdAsc(Long assessmentId);

    @Query("""
        select g
        from AssessmentGrade g
        where g.assessment.id = :assessmentId
            and g.userId in :userIds
    """)
    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    List<AssessmentGrade> findByAssessmentIdAndUserIdIn(
        @Param("assessmentId") Long assessmentId,
        @Param("userIds") Collection<Long> userIds
    );

    @Query("""
        select g
        from AssessmentGrade g
        where g.assessment.id in :assessmentIds
            and g.userId in :userIds
    """)
    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    List<AssessmentGrade> findByAssessmentIdInAndUserIdIn(
        @Param("assessmentIds") Collection<Long> assessmentIds,
        @Param("userIds") Collection<Long> userIds
    );

    @Query("""
        select g
        from AssessmentGrade g
        where g.assessment.group.id = :groupId
            and g.assessment.deletedAt is null
    """)
    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    List<AssessmentGrade> findActiveByGroupId(@Param("groupId") Long groupId);

}
