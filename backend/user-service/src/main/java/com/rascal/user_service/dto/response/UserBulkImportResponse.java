package com.rascal.user_service.dto.response;

import java.util.List;

public record UserBulkImportResponse(
    int createdCount,
    int skippedCount,
    int failedCount,
    List<UserImportRowReport> rows
) { }
