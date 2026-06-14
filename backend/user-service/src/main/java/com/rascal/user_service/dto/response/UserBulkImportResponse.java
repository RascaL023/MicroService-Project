package com.rascal.user_service.dto.response;

import java.util.List;

public record UserBulkImportResponse(
    int importedCount,
    int failedCount,
    List<UserImportRowError> errors
) { }
