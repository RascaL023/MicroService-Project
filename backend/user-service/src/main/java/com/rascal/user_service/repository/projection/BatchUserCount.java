package com.rascal.user_service.repository.projection;

public interface BatchUserCount {

    Integer getBatchId();

    long getUserCount();
}
