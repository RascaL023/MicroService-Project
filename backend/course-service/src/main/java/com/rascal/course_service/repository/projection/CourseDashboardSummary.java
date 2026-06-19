package com.rascal.course_service.repository.projection;

public interface CourseDashboardSummary {

    long getActiveGroups();

    long getPassedGroups();

    long getSubjects();

    long getInstructors();

    long getLearners();

    long getGroupsWithoutInstructor();

    long getGroupsWithoutSchedule();

}
