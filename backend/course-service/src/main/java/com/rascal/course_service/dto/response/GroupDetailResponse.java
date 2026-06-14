package com.rascal.course_service.dto.response;

import java.util.List;

public record GroupDetailResponse(
    GroupResponse group,
    List<GroupScheduleResponse> schedules,
    List<GroupMemberResponse> members
) { }
