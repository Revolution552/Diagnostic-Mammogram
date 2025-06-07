package com.diagnostic.mammogram.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path rootLocation;
    private final String baseImageUrl;

    public ImageStorageService(
            @Value("${app.image.upload-dir}") String uploadDir,
            @Value("${app.image.base-url:/images/}") String baseImageUrl) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseImageUrl = baseImageUrl.endsWith("/") ? baseImageUrl : baseImageUrl + "/";
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + fileExtension;

            Path destinationFile = this.rootLocation.resolve(filename).normalize();

            // Security check
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new StorageException("Cannot store file outside current directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.createDirectories(rootLocation);
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                return filename;
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Resource loadImage(String filename) {
        try {
            if (filename == null || filename.isBlank()) {
                throw new StorageFileNotFoundException("Filename cannot be empty");
            }

            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new StorageFileNotFoundException("Attempted directory traversal attack");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    public String getImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        // Validate filename doesn't contain path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new StorageException("Invalid filename format");
        }

        return baseImageUrl + filename;
    }

    // Custom exceptions
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class StorageFileNotFoundException extends StorageException {
        public StorageFileNotFoundException(String message) {
            super(message);
        }
        public StorageFileNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}