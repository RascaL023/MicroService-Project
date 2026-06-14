package com.rascal.user_service.dto.mapper;

import com.rascal.user_service.dto.request.BatchRequest;
import com.rascal.user_service.dto.response.BatchResponse;
import com.rascal.user_service.entity.Batch;

public final class BatchMapper {

    private BatchMapper() {}

    public static BatchResponse toResponse(Batch batch, long userCount) {
        return new BatchResponse(
            batch.getId(),
            batch.getName(),
            userCount
        );
    }

    public static Batch toEntity(BatchRequest request) {
        Batch batch = new Batch();
        batch.setId(request.id());
        batch.setName(request.name());

        return batch;
    }
}
