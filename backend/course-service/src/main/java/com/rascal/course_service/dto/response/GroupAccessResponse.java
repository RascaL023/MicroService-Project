package com.rascal.course_service.dto.response;

public record GroupAccessResponse(
    Long userId,
    Long groupId,
    String role,
    boolean isMember,
    boolean isInstructor,
    boolean hasGlobalAccess,
    boolean canRead,
    boolean canManage
) { }
