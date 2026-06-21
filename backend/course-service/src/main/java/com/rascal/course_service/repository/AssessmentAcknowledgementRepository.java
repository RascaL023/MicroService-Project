package com.rascal.course_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.AssessmentAcknowledgement;

public interface AssessmentAcknowledgementRepository extends JpaRepository<AssessmentAcknowledgement, Long> {

    @EntityGraph(attributePaths = {"assessment", "assessment.group"})
    Optional<AssessmentAcknowledgement> findByAssessmentIdAndUserId(Long assessmentId, Long userId);

    @Query("""
        SELECT ack
        FROM AssessmentAcknowledgement ack
        WHERE ack.deletedAt IS NULL
            AND ack.userId = :userId
            AND ack.assessment.id IN :assessmentIds
    """)
    List<AssessmentAcknowledgement> findActiveByAssessmentIdsAndUserId(
        @Param("assessmentIds") Collection<Long> assessmentIds,
        @Param("userId") Long userId
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM assessments a
        JOIN groups g ON g.id = a.group_id
        JOIN enrollments e ON e.group_id = g.id
            AND e.user_id = :userId
            AND e.role = 'LEARNER'
            AND e.deleted_at IS NULL
        JOIN course_user_cache cuc ON cuc.id = e.user_id
            AND cuc.deleted_at IS NULL
            AND COALESCE(cuc.status, 'ACTIVE') <> 'DROP_OUT'
        LEFT JOIN assessment_acknowledgements ack ON ack.assessment_id = a.id
            AND ack.user_id = :userId
            AND ack.deleted_at IS NULL
        LEFT JOIN assessment_grades grade ON grade.assessment_id = a.id
            AND grade.user_id = :userId
        WHERE a.deleted_at IS NULL
            AND g.deleted_at IS NULL
            AND g.status = 'ON_GOING'
            AND a.type IN ('ASSIGNMENT', 'QUIZ')
            AND ack.id IS NULL
            AND grade.id IS NULL
        """, nativeQuery = true)
    long countLearnerPendingAcknowledgements(@Param("userId") Long userId);

    @Query(value = """
        SELECT COUNT(*)
        FROM assessments a
        JOIN groups g ON g.id = a.group_id
        JOIN enrollments instructor ON instructor.group_id = g.id
            AND instructor.user_id = :userId
            AND instructor.role = 'INSTRUCTOR'
            AND instructor.deleted_at IS NULL
        JOIN enrollments learner ON learner.group_id = g.id
            AND learner.role = 'LEARNER'
            AND learner.deleted_at IS NULL
        JOIN course_user_cache cuc ON cuc.id = learner.user_id
            AND cuc.deleted_at IS NULL
            AND COALESCE(cuc.status, 'ACTIVE') <> 'DROP_OUT'
        LEFT JOIN assessment_grades grade ON grade.assessment_id = a.id
            AND grade.user_id = learner.user_id
        WHERE a.deleted_at IS NULL
            AND g.deleted_at IS NULL
            AND g.status = 'ON_GOING'
            AND grade.id IS NULL
        """, nativeQuery = true)
    long countInstructorPendingGrades(@Param("userId") Long userId);

}
