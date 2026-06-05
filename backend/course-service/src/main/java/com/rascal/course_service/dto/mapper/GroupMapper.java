package com.rascal.course_service.dto.mapper;

import java.time.LocalDateTime;

import com.rascal.course_service.dto.request.GroupPatchRequest;
import com.rascal.course_service.dto.request.GroupRequest;
import com.rascal.course_service.dto.response.GroupResponse;
import com.rascal.course_service.entity.Group;

public final class GroupMapper {

    private GroupMapper() { }

    public static Group toEntity(GroupRequest request) {
        Group group = new Group();
        group.setName(request.name());
        group.setBatchId(request.batchId());

        return group;
    }

    public static GroupResponse toResponse(Group group) {
        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getBatchId()
        );
    }

    public static void updateEntity(Group group, GroupPatchRequest request) {
        if (request.name() != null) group.setName(request.name());
        if (request.batchId() != null) group.setBatchId(request.batchId());

        group.setUpdatedAt(LocalDateTime.now());
    }
}
