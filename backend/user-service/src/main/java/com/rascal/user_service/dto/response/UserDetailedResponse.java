package com.rascal.user_service.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserDetailedResponse(
    Long id,
    String name,
    String email,
    String batch,
    String majorId,
    String majorName,
    String gender,
    String status,
    LocalDate graduatedAt,
    LocalDateTime createdAt
) { }
