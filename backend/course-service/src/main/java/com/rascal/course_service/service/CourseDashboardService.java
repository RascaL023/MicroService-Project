package com.rascal.course_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.response.CourseDashboardReminderResponse;
import com.rascal.course_service.dto.response.CourseDashboardSummaryResponse;
import com.rascal.course_service.repository.AssessmentAcknowledgementRepository;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.projection.CourseDashboardSummary;

@Service
public class CourseDashboardService {

    private final GroupRepository groupRepository;
    private final AssessmentAcknowledgementRepository assessmentAcknowledgementRepository;
    private final CurrentUserService currentUserService;

    public CourseDashboardService(
        GroupRepository groupRepository,
        AssessmentAcknowledgementRepository assessmentAcknowledgementRepository,
        CurrentUserService currentUserService
    ) {
        this.groupRepository = groupRepository;
        this.assessmentAcknowledgementRepository = assessmentAcknowledgementRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public CourseDashboardSummaryResponse getSummary() {
        CourseDashboardSummary summary = groupRepository.getDashboardSummary();

        return new CourseDashboardSummaryResponse(
            summary.getActiveGroups(),
            summary.getSubjects(),
            summary.getInstructors(),
            summary.getGroupsWithoutInstructor(),
            summary.getGroupsWithoutSchedule()
        );
    }

    @Transactional(readOnly = true)
    public CourseDashboardReminderResponse getReminders() {
        Long userId = currentUserService.getUserId();

        return new CourseDashboardReminderResponse(
            assessmentAcknowledgementRepository.countLearnerPendingAcknowledgements(userId),
            assessmentAcknowledgementRepository.countInstructorPendingGrades(userId)
        );
    }
}
