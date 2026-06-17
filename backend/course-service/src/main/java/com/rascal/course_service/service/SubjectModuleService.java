package com.rascal.course_service.service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectModule;
import com.rascal.course_service.repository.SubjectModuleRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class SubjectModuleService {

    private static final String STORAGE_DIRECTORY = "subjects";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "ppt", "pptx", "doc", "docx");
    private static final Map<String, String> MIME_TYPES = Map.of(
        "pdf", "application/pdf",
        "ppt", "application/vnd.ms-powerpoint",
        "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "doc", "application/msword",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final SubjectModuleRepository subjectModuleRepository;
    private final SubjectRepository subjectRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageService.FileRule fileRule;

    public SubjectModuleService(
        SubjectModuleRepository subjectModuleRepository,
        SubjectRepository subjectRepository,
        FileStorageService fileStorageService
    ) {
        this.subjectModuleRepository = subjectModuleRepository;
        this.subjectRepository = subjectRepository;
        this.fileStorageService = fileStorageService;
        this.fileRule = new FileStorageService.FileRule(
            true,
            ALLOWED_EXTENSIONS,
            MIME_TYPES,
            "File type must be PDF, PowerPoint, or Microsoft Word",
            "Failed to store subject module file"
        );
    }

    @Transactional(readOnly = true)
    public Page<SubjectModule> getAllPaged(Long subjectId, String filename, Pageable pageable) {
        return subjectModuleRepository.searchActiveModules(
            subjectId,
            normalizeSearchFilename(filename),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public SubjectModule getById(Long id) {
        return subjectModuleRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Subject module not found"));
    }

    @Transactional(readOnly = true)
    public Resource getFileResource(Long id) {
        SubjectModule module = getById(id);
        return getFileResource(module);
    }

    public Resource getFileResource(SubjectModule module) {
        return fileStorageService.load(physicalPath(module), "Subject module file not found");
    }

    public SubjectModule create(Long subjectId, MultipartFile file) {
        Subject subject = getActiveSubject(subjectId);
        FileStorageService.StoredFile storedFile = fileStorageService.store(
            STORAGE_DIRECTORY,
            subject.getId(),
            file,
            fileRule
        );

        SubjectModule module = new SubjectModule();
        module.setSubject(subject);
        module.setOriginalFilename(storedFile.originalFilename());
        module.setStoredFilename(storedFile.storedFilename());
        module.setFilePath(storedFile.publicPath());
        module.setMimeType(storedFile.mimeType());
        module.setFileSize(storedFile.fileSize());
        module.setCreatedAt(LocalDateTime.now());

        return subjectModuleRepository.save(module);
    }

    public SubjectModule updateById(Long id, Long subjectId, MultipartFile file) {
        if (subjectId == null && (file == null || file.isEmpty()))
            throw new BadRequestException("Invalid patch");

        SubjectModule module = getById(id);
        Long oldSubjectId = module.getSubject().getId();
        Subject subject = subjectId == null ? module.getSubject() : getActiveSubject(subjectId);
        Path oldFile = physicalPath(module);

        module.setSubject(subject);
        if (file != null && !file.isEmpty()) {
            FileStorageService.StoredFile storedFile = fileStorageService.store(
                STORAGE_DIRECTORY,
                subject.getId(),
                file,
                fileRule
            );
            module.setOriginalFilename(storedFile.originalFilename());
            module.setStoredFilename(storedFile.storedFilename());
            module.setFilePath(storedFile.publicPath());
            module.setMimeType(storedFile.mimeType());
            module.setFileSize(storedFile.fileSize());
            fileStorageService.delete(oldFile);
        } else if (!oldSubjectId.equals(subject.getId())) {
            fileStorageService.move(
                oldFile,
                STORAGE_DIRECTORY,
                subject.getId(),
                module.getStoredFilename(),
                "Failed to move subject module file"
            );
            module.setFilePath(fileStorageService.publicPath(
                STORAGE_DIRECTORY,
                subject.getId(),
                module.getStoredFilename()
            ));
        }

        return subjectModuleRepository.save(module);
    }

    public void deleteById(Long id) {
        SubjectModule module = getById(id);
        module.setDeletedAt(LocalDateTime.now());

        subjectModuleRepository.save(module);
        fileStorageService.delete(physicalPath(module));
    }

    private Subject getActiveSubject(Long subjectId) {
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private Path physicalPath(SubjectModule module) {
        return fileStorageService.physicalPath(
            STORAGE_DIRECTORY,
            module.getSubject().getId(),
            module.getStoredFilename()
        );
    }

    private String normalizeSearchFilename(String filename) {
        if (filename == null || filename.isBlank()) return "";

        return filename.trim();
    }

}
