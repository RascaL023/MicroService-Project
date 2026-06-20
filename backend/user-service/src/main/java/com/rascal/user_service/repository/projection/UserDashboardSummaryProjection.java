package com.rascal.user_service.repository.projection;

public interface UserDashboardSummaryProjection {
    Long getTotalUsers();
    Long getTotalBatches();
    Long getTotalMajors();
}
