package com.rascal.course_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.response.CourseDashboardSummaryResponse;
import com.rascal.course_service.repository.GroupRepository;
import com.rascal.course_service.repository.projection.CourseDashboardSummary;

@Service
public class CourseDashboardService {

    private final GroupRepository groupRepository;

    public CourseDashboardService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
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
}
