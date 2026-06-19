package com.rascal.course_service.repository.projection;

public interface CourseDashboardSummary {

    long getActiveGroups();

    long getSubjects();

    long getInstructors();

    long getGroupsWithoutInstructor();

    long getGroupsWithoutSchedule();

}
