package com.rascal.user_service.dto.response;

public record BatchResponse(
    Integer id,
    String name,
    long userCount
) { }
