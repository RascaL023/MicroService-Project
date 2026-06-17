package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.AssessmentGradeResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.AssessmentGrade;

public final class AssessmentGradeMapper {

    private AssessmentGradeMapper() { }

    public static AssessmentGradeResponse toResponse(AssessmentGrade grade, UserLookupResponse user) {
        UserLookupResponse resolvedUser = user == null
            ? new UserLookupResponse(grade.getUserId(), null, null, null)
            : user;

        return new AssessmentGradeResponse(
            grade.getId(),
            grade.getAssessment().getId(),
            resolvedUser,
            grade.getScore(),
            grade.getFeedback(),
            grade.getGradedBy(),
            grade.getGradedAt(),
            grade.getUpdatedAt()
        );
    }
}
