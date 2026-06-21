package com.rascal.course_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.mapper.AssessmentGradeMapper;
import com.rascal.course_service.dto.mapper.AssessmentMapper;
import com.rascal.course_service.dto.mapper.GroupMapper;
import com.rascal.course_service.dto.request.AssessmentGradeBatchRequest;
import com.rascal.course_service.dto.request.AssessmentGradeRequest;
import com.rascal.course_service.dto.response.AssessmentGradeResponse;
import com.rascal.course_service.dto.response.GroupGradebookResponse;
import com.rascal.course_service.dto.response.GroupMemberResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.entity.AssessmentGrade;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.enumerated.CourseRoleEnum;
import com.rascal.course_service.repository.AssessmentGradeRepository;
import com.rascal.course_service.repository.AssessmentRepository;
import com.rascal.course_service.repository.EnrollmentRepository;
import com.rascal.course_service.repository.GroupRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class AssessmentGradeService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private final AssessmentGradeRepository assessmentGradeRepository;
    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;
    private final CoursePermissionService coursePermissionService;
    private final CurrentUserService currentUserService;
    private final CourseUserCacheService courseUserCacheService;

    public AssessmentGradeService(
        AssessmentGradeRepository assessmentGradeRepository,
        AssessmentRepository assessmentRepository,
        EnrollmentRepository enrollmentRepository,
        GroupRepository groupRepository,
        CoursePermissionService coursePermissionService,
        CurrentUserService currentUserService,
        CourseUserCacheService courseUserCacheService
    ) {
        this.assessmentGradeRepository = assessmentGradeRepository;
        this.assessmentRepository = assessmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.groupRepository = groupRepository;
        this.coursePermissionService = coursePermissionService;
        this.currentUserService = currentUserService;
        this.courseUserCacheService = courseUserCacheService;
    }

    @Transactional(readOnly = true)
    public List<AssessmentGradeResponse> getByAssessmentId(Long assessmentId) {
        // Assessment assessment = getActiveAssessment(assessmentId);
        // coursePermissionService.requireGroupInstructor(assessment.getGroup().getId());

        List<AssessmentGrade> grades = assessmentGradeRepository.findByAssessmentIdOrderByUserIdAsc(assessmentId);
        Map<Long, UserLookupResponse> usersById = lookupUsersById(
            grades.stream().map(AssessmentGrade::getUserId).toList()
        );

        return grades.stream()
            .map(grade -> AssessmentGradeMapper.toResponse(grade, usersById.get(grade.getUserId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public GroupGradebookResponse getGradebookByGroupId(Long groupId) {
        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));

        List<Enrollment> learners = enrollmentRepository
            .findByGroupIdAndRoleAndDeletedAtIsNull(groupId, CourseRoleEnum.LEARNER);
        Map<Long, UserLookupResponse> usersById = lookupUsersById(
            learners.stream().map(Enrollment::getUserId).toList()
        );
        List<GroupMemberResponse> members = learners.stream()
            .map(enrollment -> toMemberResponse(enrollment, usersById.get(enrollment.getUserId())))
            .toList();

        List<Assessment> assessments = assessmentRepository.findActiveByGroupIdOrderByDueAt(groupId);
        List<AssessmentGrade> grades = assessmentGradeRepository.findActiveByGroupId(groupId);

        return new GroupGradebookResponse(
            GroupMapper.toResponse(group, group.getSubject()),
            members,
            assessments.stream()
                .map(AssessmentMapper::toResponse)
                .toList(),
            grades.stream()
                .map(grade -> AssessmentGradeMapper.toResponse(grade, usersById.get(grade.getUserId())))
                .toList()
        );
    }

    public List<AssessmentGradeResponse> upsertBatch(Long assessmentId, AssessmentGradeBatchRequest request) {
        Assessment assessment = getActiveAssessment(assessmentId);
        coursePermissionService.requireGroupInstructor(assessment.getGroup().getId());

        if (request.grades() == null || request.grades().isEmpty())
            throw new BadRequestException("Grades must be filled");

        validateUniqueUsers(request.grades());

        Long groupId = assessment.getGroup().getId();
        Long graderId = currentUserService.getUserId();
        LocalDateTime now = LocalDateTime.now();
        List<Long> userIds = request.grades().stream()
            .map(AssessmentGradeRequest::userId)
            .distinct()
            .toList();

        Map<Long, Enrollment> learnersByUserId = enrollmentRepository
            .findByGroupIdAndRoleAndDeletedAtIsNull(groupId, CourseRoleEnum.LEARNER)
            .stream()
            .collect(Collectors.toMap(Enrollment::getUserId, Function.identity()));
        for (Long userId : userIds) {
            if (!learnersByUserId.containsKey(userId))
                throw new BadRequestException("User " + userId + " is not a learner in this group");
        }

        Map<Long, AssessmentGrade> existingByUserId = assessmentGradeRepository
            .findByAssessmentIdAndUserIdIn(assessmentId, userIds)
            .stream()
            .collect(Collectors.toMap(AssessmentGrade::getUserId, Function.identity()));

        List<AssessmentGrade> grades = request.grades().stream()
            .map(item -> {
                AssessmentGrade grade = existingByUserId.getOrDefault(item.userId(), new AssessmentGrade());
                if (grade.getId() == null) {
                    grade.setAssessment(assessment);
                    grade.setUserId(item.userId());
                    grade.setGradedAt(now);
                } else {
                    grade.setUpdatedAt(now);
                }

                grade.setScore(normalizeScore(item.score()));
                grade.setFeedback(normalizeFeedback(item.feedback()));
                grade.setGradedBy(graderId);

                return grade;
            })
            .toList();

        List<AssessmentGrade> saved = assessmentGradeRepository.saveAll(grades);
        Map<Long, UserLookupResponse> usersById = lookupUsersById(
            saved.stream().map(AssessmentGrade::getUserId).toList()
        );

        return saved.stream()
            .map(grade -> AssessmentGradeMapper.toResponse(grade, usersById.get(grade.getUserId())))
            .toList();
    }

    private Assessment getActiveAssessment(Long assessmentId) {
        return assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)
            .orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) throw new BadRequestException("Score must be filled");
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0)
            throw new BadRequestException("Score must be between 0 and 100");

        return score;
    }

    private void validateUniqueUsers(List<AssessmentGradeRequest> grades) {
        Set<Long> userIds = new HashSet<>();
        for (AssessmentGradeRequest grade : grades) {
            if (grade.userId() == null) throw new BadRequestException("User ID must be filled");
            if (!userIds.add(grade.userId()))
                throw new BadRequestException("Duplicate learner in grade request");
        }
    }

    private String normalizeFeedback(String feedback) {
        if (feedback == null) return null;

        String normalized = feedback.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Map<Long, UserLookupResponse> lookupUsersById(List<Long> userIds) {
        return courseUserCacheService.lookupByIds(userIds);
    }

    private GroupMemberResponse toMemberResponse(Enrollment enrollment, UserLookupResponse user) {
        UserLookupResponse resolvedUser = user == null
            ? new UserLookupResponse(enrollment.getUserId(), null, null, null)
            : user;

        return new GroupMemberResponse(
            enrollment.getId(),
            resolvedUser,
            enrollment.getRole().getDisplayName()
        );
    }
}
