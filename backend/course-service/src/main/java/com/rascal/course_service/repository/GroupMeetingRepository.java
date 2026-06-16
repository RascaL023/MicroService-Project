package com.rascal.course_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rascal.course_service.entity.GroupMeeting;
import com.rascal.course_service.enumerated.GroupMeetingStatusEnum;

public interface GroupMeetingRepository extends JpaRepository<GroupMeeting, Long> {

    @EntityGraph(attributePaths = {"group", "group.subject", "subjectMaterial", "subjectMaterial.subject"})
    Optional<GroupMeeting> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"group", "group.subject", "subjectMaterial", "subjectMaterial.subject"})
    Optional<GroupMeeting> findByGroupIdAndSubjectMaterialIdAndDeletedAtIsNull(Long groupId, Long subjectMaterialId);

    boolean existsByGroupIdAndSubjectMaterialIdAndDeletedAtIsNull(Long groupId, Long subjectMaterialId);

    @EntityGraph(attributePaths = {"group", "group.subject", "subjectMaterial", "subjectMaterial.subject"})
    List<GroupMeeting> findByGroupIdAndDeletedAtIsNullOrderBySubjectMaterialMeetingNumberAsc(Long groupId);

    @Query("""
        SELECT m
        FROM GroupMeeting m
        WHERE m.deletedAt IS NULL
            AND (:groupId IS NULL OR m.group.id = :groupId)
            AND (:subjectId IS NULL OR m.group.subject.id = :subjectId)
            AND (:status IS NULL OR m.status = :status)
    """)
    @EntityGraph(attributePaths = {"group", "group.subject", "subjectMaterial", "subjectMaterial.subject"})
    Page<GroupMeeting> searchActiveMeetings(
        @Param("groupId") Long groupId,
        @Param("subjectId") Long subjectId,
        @Param("status") GroupMeetingStatusEnum status,
        Pageable pageable
    );

}
