package com.rascal.course_service.dto.response;

import java.util.List;

public record GroupGradebookResponse(
    GroupResponse group,
    List<GroupMemberResponse> members,
    List<AssessmentResponse> assessments,
    List<AssessmentGradeResponse> grades
) { }
