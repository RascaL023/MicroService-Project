package com.rascal.course_service.dto.response;

public record UserLookupResponse(
    Long id,
    String name,
    Character gender,
    Integer batch,
    String status
) {
    public UserLookupResponse(Long id, String name, Character gender, Integer batch) {
        this(id, name, gender, batch, "ACTIVE");
    }
}
