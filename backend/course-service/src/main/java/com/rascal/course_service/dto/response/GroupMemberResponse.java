package com.rascal.course_service.dto.response;

public record GroupMemberResponse(
    Long enrollmentId,
    UserLookupResponse user,
    String role
) { }
