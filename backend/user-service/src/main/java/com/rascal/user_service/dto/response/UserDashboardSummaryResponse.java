package com.rascal.user_service.dto.response;

public record UserDashboardSummaryResponse(
    long totalUsers,
    long totalBatches,
    long totalMajors
) {}
