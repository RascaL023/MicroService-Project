package com.rascal.course_service.dto.response;

public record ApiSuccessResponse<T>(
    Boolean isSuccess,
    String message,
    T data
) { }
