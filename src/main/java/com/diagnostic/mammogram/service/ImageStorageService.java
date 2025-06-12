package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.exception.ImageStorageException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * Interface for managing storage operations for image files.
 */
public interface ImageStorageService {

    // Removed: void init();
    // The initialization logic is now handled in the constructor of concrete implementations
    // like FileSystemStorageService, typically based on injected properties.

    /**
     * Stores a file on the configured storage location.
     *
     * @param file The MultipartFile received from the client.
     * @param subFolder An optional sub-folder path (e.g., "mammograms/patientId") to organize files.
     * @return The unique path/URL of the stored file.
     * @throws ImageStorageException if the file cannot be stored.
     */
    String storeFile(MultipartFile file, String subFolder);

    /**
     * Loads a stored file as a Spring Resource.
     *
     * @param fileName The unique path/name of the file to load.
     * @return A Spring Resource representing the file.
     * @throws ImageStorageException if the file cannot be found or accessed.
     */
    Resource loadFileAsResource(String fileName);

    /**
     * Deletes a file from the configured storage location.
     *
     * @param filePath The unique path/URL of the file to delete.
     * @throws ImageStorageException if the file cannot be deleted.
     */
    void deleteFile(String filePath);

    /**
     * Deletes all files and sub-directories from the root storage location.
     * Use with extreme caution, primarily for testing or cleanup.
     */
    void deleteAll();

    /**
     * Resolves the Path for a given file name within the storage root.
     *
     * @param filename The name of the file.
     * @return The absolute Path to the file.
     */
    Path resolvePath(String filename);
}