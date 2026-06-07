package com.rascal.course_service.dto.response;

public record UserLookupResponse(
    Long id,
    String name,
    Character gender,
    Integer batch
) { }

