package com.rascal.course_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final String PUBLIC_UPLOAD_PREFIX = "/app/uploads";

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(
        String directory,
        Long ownerId,
        MultipartFile file,
        FileRule rule
    ) {
        validateFile(file, rule);

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = fileExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path ownerDir = ownerDirectory(directory, ownerId);
        Path target = ownerDir.resolve(storedFilename).normalize();

        if (!target.startsWith(ownerDir))
            throw new BadRequestException("Invalid file path");

        try {
            Files.createDirectories(ownerDir);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) { throw new BadRequestException(rule.storeErrorMessage()); }

        return new StoredFile(
            originalFilename,
            storedFilename,
            publicPath(directory, ownerId, storedFilename),
            rule.mimeTypes().get(extension),
            file.getSize()
        );
    }

    public Resource load(Path path, String notFoundMessage) {
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new NotFoundException(notFoundMessage);

            return resource;
        } catch (MalformedURLException ex) { throw new NotFoundException(notFoundMessage); }
    }

    public Path physicalPath(String directory, Long ownerId, String storedFilename) {
        return ownerDirectory(directory, ownerId)
            .resolve(storedFilename)
            .normalize();
    }

    public void move(Path oldFile, String directory, Long newOwnerId, String storedFilename, String errorMessage) {
        Path newOwnerDir = ownerDirectory(directory, newOwnerId);
        Path newFile = newOwnerDir.resolve(storedFilename).normalize();

        if (!newFile.startsWith(newOwnerDir))
            throw new BadRequestException("Invalid file path");

        try {
            Files.createDirectories(newOwnerDir);
            Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) { throw new BadRequestException(errorMessage); }
    }

    public void delete(Path path) {
        if (path == null) return;

        try { Files.deleteIfExists(path); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public String publicPath(String directory, Long ownerId, String storedFilename) {
        return PUBLIC_UPLOAD_PREFIX + "/" + directory + "/" + ownerId + "/" + storedFilename;
    }

    private void validateFile(MultipartFile file, FileRule rule) {
        if (file == null || file.isEmpty()) {
            if (rule.required()) throw new BadRequestException("File must be filled");
            return;
        }

        if (file.getSize() > MAX_FILE_SIZE)
            throw new BadRequestException("Maximum file size is 5MB");

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = fileExtension(originalFilename);
        if (!rule.allowedExtensions().contains(extension))
            throw new BadRequestException(rule.invalidTypeMessage());
    }

    private Path ownerDirectory(String directory, Long ownerId) {
        Path ownerDir = uploadRoot.resolve(directory).resolve(String.valueOf(ownerId)).normalize();
        if (!ownerDir.startsWith(uploadRoot))
            throw new BadRequestException("Invalid file path");

        return ownerDir;
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

    public record FileRule(
        boolean required,
        Set<String> allowedExtensions,
        Map<String, String> mimeTypes,
        String invalidTypeMessage,
        String storeErrorMessage
    ) { }

    public record StoredFile(
        String originalFilename,
        String storedFilename,
        String publicPath,
        String mimeType,
        Long fileSize
    ) { }

}
