package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.exception.ImageStorageException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * Interface for managing storage operations for image files.
 */
public interface ImageStorageService {

    // The initialization logic is now handled in the constructor of concrete implementations
    // like FileSystemStorageService, typically based on injected properties.

    /**
     * Stores a file on the configured storage location.
     *
     * @param file The MultipartFile received from the client.
     * @param subFolder An optional sub-folder path (e.g., "mammograms/patientId") to organize files.
     * @return The unique relative path of the stored file (e.g., "mammograms/1/filename.png").
     * @throws ImageStorageException if the file cannot be stored.
     */
    String storeFile(MultipartFile file, String subFolder);

    /**
     * Loads a stored file as a Spring Resource.
     *
     * @param fileName The unique path/name of the file (relative to the storage root) to load.
     * @return A Spring Resource representing the file.
     * @throws ImageStorageException if the file cannot be found or accessed.
     */
    Resource loadFileAsResource(String fileName);

    /**
     * Deletes a file from the configured storage location.
     *
     * @param absoluteFilePath The absolute file system path of the file to delete.
     * @throws ImageStorageException if the file cannot be deleted.
     */
    void deleteFile(String absoluteFilePath);

    /**
     * Deletes all files and sub-directories from the root storage location.
     * Use with extreme caution, primarily for testing or cleanup.
     */
    void deleteAll();

    /**
     * Resolves the Path for a given file name (expected to be relative to the storage root)
     * into an absolute {@link Path} within the storage root.
     *
     * @param filename The relative path/name of the file.
     * @return The absolute {@link Path} to the file.
     */
    Path resolvePath(String filename);

    /**
     * Returns the absolute file system path for a given relative path.
     * This is intended for internal use by services that need direct file system access.
     *
     * @param relativePath The path of the file relative to the storage root (e.g., "mammograms/1/filename.png").
     * @return The absolute file system path (e.g., "C:\ uploads\mammograms\1\filename.png").
     */
    String getAbsoluteFilePath(String relativePath);

    /**
     * Returns the public URL for a given relative path.
     * This is intended for use by the frontend or other external clients.
     *
     * @param relativePath The path of the file relative to the storage root (e.g., "mammograms/1/filename.png").
     * @return The full URL (e.g., "http://localhost:8080/images/mammograms/1/filename.png") or the relative path if no base URL is configured.
     */
    String getFileUrl(String relativePath);
}
