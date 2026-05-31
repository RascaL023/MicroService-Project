package com.rascal.user_service.dto.response;

public record UserResponse(
    Long id,
    String name,
    String email,
    Integer batch,
    String gender,
    boolean isBanned
) { }
