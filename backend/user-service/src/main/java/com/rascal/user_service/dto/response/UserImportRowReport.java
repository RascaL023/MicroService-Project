package com.rascal.user_service.dto.response;

public record UserImportRowReport(
    int row,
    String email,
    String status,
    String message
) { }
