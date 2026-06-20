package com.rascal.user_service.dto.mapper;

import com.rascal.user_service.dto.request.MajorRequest;
import com.rascal.user_service.dto.response.MajorResponse;
import com.rascal.user_service.entity.Major;

public final class MajorMapper {

    private MajorMapper() {}

    public static Major toEntity(MajorRequest request) {
        Major major = new Major();
        major.setId(request.id());
        major.setName(request.name());
        return major;
    }

    public static MajorResponse toResponse(Major major, Long userCount) {
        return new MajorResponse(
            major.getId(),
            major.getName(),
            userCount
        );
    }
}
