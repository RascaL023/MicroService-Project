package com.rascal.course_service.dto.response;

public record SubjectModuleResponse(
    Long id,
    Long subjectId,
    String subjectName,
    String originalFilename,
    String storedFilename,
    String filePath,
    String mimeType,
    Long fileSize
) { }
