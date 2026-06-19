package com.rascal.user_service.dto.response;

import java.time.LocalDateTime;

public record UserDetailResponse(
    Long id,
    String name,
    String email,
    String batch,
    String gender,
    String status,
    LocalDateTime createdAt
) { }
