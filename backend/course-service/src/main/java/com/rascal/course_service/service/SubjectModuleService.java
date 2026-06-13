package com.rascal.course_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final String PUBLIC_UPLOAD_PREFIX = "/app/uploads";
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
    private final Path uploadRoot;

    public SubjectModuleService(
        SubjectModuleRepository subjectModuleRepository,
        SubjectRepository subjectRepository,
        @Value("${app.storage.upload-dir:uploads}") String uploadDir
    ) {
        this.subjectModuleRepository = subjectModuleRepository;
        this.subjectRepository = subjectRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
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
        Path file = physicalPath(module);

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new NotFoundException("Subject module file not found");

            return resource;
        } catch (MalformedURLException ex) {
            throw new NotFoundException("Subject module file not found");
        }
    }

    public SubjectModule create(Long subjectId, MultipartFile file) {
        Subject subject = getActiveSubject(subjectId);
        StoredFile storedFile = storeFile(subject.getId(), file);

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
            StoredFile storedFile = storeFile(subject.getId(), file);
            module.setOriginalFilename(storedFile.originalFilename());
            module.setStoredFilename(storedFile.storedFilename());
            module.setFilePath(storedFile.publicPath());
            module.setMimeType(storedFile.mimeType());
            module.setFileSize(storedFile.fileSize());
            deletePhysicalFile(oldFile);
        } else if (!oldSubjectId.equals(subject.getId())) {
            moveExistingFile(oldFile, subject.getId(), module.getStoredFilename());
            module.setFilePath(publicPath(subject.getId(), module.getStoredFilename()));
        }

        return subjectModuleRepository.save(module);
    }

    public void deleteById(Long id) {
        SubjectModule module = getById(id);
        module.setDeletedAt(LocalDateTime.now());

        subjectModuleRepository.save(module);
        deletePhysicalFile(physicalPath(module));
    }

    private Subject getActiveSubject(Long subjectId) {
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private StoredFile storeFile(Long subjectId, MultipartFile file) {
        validateFile(file);

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = fileExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path subjectDir = uploadRoot.resolve("subjects").resolve(String.valueOf(subjectId)).normalize();
        Path target = subjectDir.resolve(storedFilename).normalize();

        if (!target.startsWith(subjectDir))
            throw new BadRequestException("Invalid file path");

        try {
            Files.createDirectories(subjectDir);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store subject module file");
        }

        return new StoredFile(
            originalFilename,
            storedFilename,
            publicPath(subjectId, storedFilename),
            MIME_TYPES.get(extension),
            file.getSize()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("File must be filled");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new BadRequestException("Maximum file size is 5MB");

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = fileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new BadRequestException("File type must be PDF, PowerPoint, or Microsoft Word");
    }

    private String sanitizeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank())
            throw new BadRequestException("Filename must be filled");

        String normalized = Paths.get(filename).getFileName().toString().trim();
        if (normalized.isBlank())
            throw new BadRequestException("Filename must be filled");

        return normalized;
    }

    private String fileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1)
            throw new BadRequestException("Invalid file name");

        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private Path physicalPath(SubjectModule module) {
        return uploadRoot.resolve("subjects")
            .resolve(String.valueOf(module.getSubject().getId()))
            .resolve(module.getStoredFilename())
            .normalize();
    }

    private void moveExistingFile(Path oldFile, Long newSubjectId, String storedFilename) {
        Path newSubjectDir = uploadRoot.resolve("subjects").resolve(String.valueOf(newSubjectId)).normalize();
        Path newFile = newSubjectDir.resolve(storedFilename).normalize();

        if (!newFile.startsWith(newSubjectDir))
            throw new BadRequestException("Invalid file path");

        try {
            Files.createDirectories(newSubjectDir);
            Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to move subject module file");
        }
    }

    private String publicPath(Long subjectId, String storedFilename) {
        return PUBLIC_UPLOAD_PREFIX + "/subjects/" + subjectId + "/" + storedFilename;
    }

    private void deletePhysicalFile(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private String normalizeSearchFilename(String filename) {
        if (filename == null || filename.isBlank()) return "";

        return filename.trim();
    }

    private record StoredFile(
        String originalFilename,
        String storedFilename,
        String publicPath,
        String mimeType,
        Long fileSize
    ) { }
}
