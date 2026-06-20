package com.rascal.user_service.dto.response;

public record MajorResponse(
    String id,
    String name,
    Long userCount
) { }
