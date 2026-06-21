package com.rascal.course_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
    name = "assessment_acknowledgements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_assessment_acknowledgements_assessment_user",
        columnNames = {"assessment_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_assessment_acknowledgements_user", columnList = "user_id, deleted_at"),
        @Index(name = "idx_assessment_acknowledgements_assessment", columnList = "assessment_id, deleted_at")
    }
)
public class AssessmentAcknowledgement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "done_at", nullable = false)
    private LocalDateTime doneAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

}
