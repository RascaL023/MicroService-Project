package com.rascal.user_service.dto.response;

public record UserLookupResponse(
    Long id,
    String name,
    Character gender,
    Integer batch
) { }
