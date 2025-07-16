package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.exception.ImageStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j // Lombok for logging
public class FileSystemStorageService implements ImageStorageService {

    private final Path rootLocation; // Declared as final
    private final String staticImageBaseUrl; // Added for returning full URLs

    /**
     * Initializes the FileSystemStorageService by setting up the root storage location.
     * The upload directory path and the base URL for serving static images are injected
     * from application properties.
     *
     * @param uploadDir The directory path where files will be stored.
     * @param staticImageBaseUrl The base URL (e.g., "http://localhost:8080/images/")
     * from which stored images will be publicly accessible.
     */
    public FileSystemStorageService(
            @Value("${file.upload-dir}") String uploadDir,
            @Value("${static.image.base-url:}") String staticImageBaseUrl) { // Default to empty string if not set
        // Initialize rootLocation directly in the constructor
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.staticImageBaseUrl = staticImageBaseUrl; // Initialize the new field

        // Initialize the storage directory
        try {
            if (Files.exists(this.rootLocation) && !Files.isDirectory(this.rootLocation)) {
                throw new ImageStorageException("Upload directory " + this.rootLocation + " exists but is not a directory.");
            }
            Files.createDirectories(this.rootLocation); // Create directory if it doesn't exist
            log.info("Initialized storage root directory: {}", this.rootLocation);
        } catch (IOException e) {
            throw new ImageStorageException("Could not initialize storage location!", e);
        }
    }

    /**
     * Stores a multipart file within the configured storage location,
     * organizing it into a specified sub-folder. A unique filename is generated.
     *
     * @param file The {@link MultipartFile} received from the client.
     * @param subFolder An optional sub-folder path (e.g., "mammograms/patientId") to organize files.
     * @return The relative path from the rootLocation (e.g., "mammograms/1/filename.png").
     * This path is intended for storage in the database.
     * @throws ImageStorageException if the file cannot be stored due to I/O errors.
     */
    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        // Validate file presence
        if (file.isEmpty()) {
            throw new ImageStorageException("Cannot store empty file.");
        }

        // Normalize file name to prevent directory traversal attacks and get extension
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        // Generate a unique file name to prevent collisions
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Resolve the target directory
        Path targetDirectory = this.rootLocation;
        if (StringUtils.hasText(subFolder)) {
            // Ensure subFolder doesn't contain path traversal attempts
            Path resolvedSubFolder = Paths.get(subFolder).normalize();
            if (resolvedSubFolder.startsWith("..")) { // Basic check for malicious paths
                throw new ImageStorageException("Invalid subFolder path: " + subFolder);
            }
            targetDirectory = this.rootLocation.resolve(resolvedSubFolder);
        }

        try {
            // Ensure the target directory exists
            Files.createDirectories(targetDirectory);

            // Construct the target file path
            Path targetLocation = targetDirectory.resolve(uniqueFileName);

            // Copy file to the target location, replacing if it somehow exists (shouldn't with UUID)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} in directory: {}", uniqueFileName, targetDirectory);

            // Always return the relative path from the rootLocation for database storage
            String relativePath = this.rootLocation.relativize(targetLocation).toString().replace("\\", "/"); // Normalize slashes for URL
            return relativePath;

        } catch (IOException ex) {
            throw new ImageStorageException("Failed to store file " + originalFilename + ". " + ex.getMessage(), ex);
        }
    }

    /**
     * Loads a stored file as a Spring {@link Resource}. This is typically used
     * when serving the file content directly via a controller.
     *
     * @param filename The unique path/name of the file (relative to rootLocation) to load.
     * @return A Spring {@link Resource} representing the file.
     * @throws ImageStorageException if the file cannot be found, is not readable, or URL is malformed.
     */
    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path file = resolvePath(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                log.debug("Resource loaded: {}", file);
                return resource;
            } else {
                // If file does not exist or is not readable, throw exception
                throw new ImageStorageException("Could not read file: " + filename + " (file not found or not readable)");
            }
        } catch (MalformedURLException ex) {
            throw new ImageStorageException("Could not read file: " + filename + ". Invalid URL format.", ex);
        }
    }

    /**
     * Deletes a specified file from the storage location.
     *
     * @param absoluteFilePath The absolute file system path of the file to delete.
     * @throws ImageStorageException if the file cannot be deleted.
     */
    @Override
    public void deleteFile(String absoluteFilePath) {
        Path fileToDelete = Paths.get(absoluteFilePath).normalize(); // Normalize the incoming absolute path

        // Ensure the path is within the rootLocation to prevent directory traversal attacks during deletion
        if (!fileToDelete.startsWith(rootLocation)) {
            throw new ImageStorageException("Attempted to delete file outside of storage root: " + absoluteFilePath);
        }

        try {
            if (Files.exists(fileToDelete) && Files.isRegularFile(fileToDelete)) {
                Files.delete(fileToDelete);
                log.info("Deleted file: {}", absoluteFilePath);
            } else {
                log.warn("Attempted to delete non-existent or non-file path: {}", absoluteFilePath);
            }
        } catch (IOException e) {
            throw new ImageStorageException("Could not delete file: " + absoluteFilePath, e);
        }
    }

    /**
     * Deletes all files and sub-directories from the root storage location.
     * Use with extreme caution, primarily for testing or cleanup operations.
     * This will effectively clear all uploaded data.
     *
     * @throws ImageStorageException if deletion or re-creation of the root directory fails.
     */
    @Override
    public void deleteAll() {
        log.warn("ATTENTION: Deleting all files and re-creating storage root: {}", rootLocation);
        try {
            FileSystemUtils.deleteRecursively(rootLocation.toFile());
            // Re-create the root directory after deletion to ensure it's ready for new uploads
            Files.createDirectories(this.rootLocation);
            log.info("Storage root directory re-created after deleteAll.");
        } catch (IOException e) {
            throw new ImageStorageException("Could not delete all files or re-create root directory!", e);
        }
    }

    /**
     * Resolves a given filename (expected to be relative to rootLocation)
     * into an absolute {@link Path} within the storage root.
     *
     * @param filename The relative path/name of the file.
     * @return The absolute {@link Path} to the file.
     */
    @Override
    public Path resolvePath(String filename) {
        // Resolve filename against the root location. It's crucial to normalize
        // to prevent '..' traversal attempts, even if StringUtils.cleanPath was used earlier.
        Path resolvedPath = rootLocation.resolve(filename).normalize();
        // Ensure the resolved path is still within the rootLocation to prevent
        // accessing files outside the designated storage area.
        if (!resolvedPath.startsWith(rootLocation)) {
            throw new ImageStorageException("Attempted to access file outside of storage root: " + filename);
        }
        return resolvedPath;
    }

    /**
     * Returns the absolute file system path for a given relative path.
     * This is intended for internal use by services that need direct file system access.
     *
     * @param relativePath The path of the file relative to the storage root (e.g., "mammograms/1/filename.png").
     * @return The absolute file system path (e.g., "C:\ uploads\mammograms\1\filename.png").
     * @throws ImageStorageException if the resolved path is outside the storage root.
     */
    @Override
    public String getAbsoluteFilePath(String relativePath) {
        return resolvePath(relativePath).toAbsolutePath().toString();
    }

    /**
     * Returns the public URL for a given relative path.
     * This is intended for use by the frontend or other external clients.
     *
     * @param relativePath The path of the file relative to the storage root (e.g., "mammograms/1/filename.png").
     * @return The full URL (e.g., "http://localhost:8080/images/mammograms/1/filename.png") or the relative path if no base URL is configured.
     */
    @Override
    public String getFileUrl(String relativePath) {
        if (StringUtils.hasText(staticImageBaseUrl)) {
            // Ensure staticImageBaseUrl ends with a slash and relativePath doesn't start with one
            String baseUrl = staticImageBaseUrl.endsWith("/") ? staticImageBaseUrl : staticImageBaseUrl + "/";
            String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            return baseUrl + path;
        }
        return relativePath; // If no base URL, return relative path (useful for internal references)
    }
}
