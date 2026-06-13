package com.rascal.course_service.dto.mapper;

import com.rascal.course_service.dto.response.SubjectModuleResponse;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectModule;

public final class SubjectModuleMapper {

    private SubjectModuleMapper() { }

    public static SubjectModuleResponse toResponse(SubjectModule module) {
        Subject subject = module.getSubject();

        return new SubjectModuleResponse(
            module.getId(),
            subject.getId(),
            subject.getName(),
            module.getOriginalFilename(),
            module.getStoredFilename(),
            module.getFilePath(),
            module.getMimeType(),
            module.getFileSize()
        );
    }
}
