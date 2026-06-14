package com.rascal.user_service.dto.response;

public record UserImportRowError(
    int row,
    String message
) { }
