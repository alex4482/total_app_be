package com.work.total_app.validators;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates file uploads for security:
 * - MIME type validation using magic bytes (not just extension)
 * - File size limits
 * - Filename sanitization (prevent path traversal)
 * - Extension whitelist
 * - Content validation
 */
@Component
@Log4j2
public class FileUploadValidator {

    // Maximum file size in bytes (configurable, default 10MB)
    @Value("${app.file-upload.max-size-bytes:10485760}")
    private long maxFileSizeBytes;

    // Allowed MIME types (configurable)
    @Value("${app.file-upload.allowed-mime-types:image/jpeg,image/png,image/gif,image/webp,application/pdf,text/plain,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/zip,application/x-zip-compressed}")
    private String allowedMimeTypesConfig;

    // Allowed file extensions (configurable)
    @Value("${app.file-upload.allowed-extensions:.jpg,.jpeg,.png,.gif,.webp,.pdf,.txt,.csv,.xls,.xlsx,.doc,.docx,.zip}")
    private String allowedExtensionsConfig;

    // Pattern for dangerous characters in filenames
    private static final Pattern DANGEROUS_FILENAME_PATTERN = Pattern.compile("[^a-zA-Z0-9._\\-\\s]");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\.[\\\\/])|(\\.\\.)");

    // Magic bytes for common file types (for MIME validation)
    private static final Map<String, byte[][]> MAGIC_BYTES = new HashMap<>();

    static {
        // Images
        MAGIC_BYTES.put("image/jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}});
        MAGIC_BYTES.put("image/png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47}});
        MAGIC_BYTES.put("image/gif", new byte[][]{
                {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, // GIF87a
                {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}  // GIF89a
        });
        MAGIC_BYTES.put("image/webp", new byte[][]{{0x52, 0x49, 0x46, 0x46}}); // RIFF (WebP)

        // Documents
        MAGIC_BYTES.put("application/pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}); // %PDF

        // Office formats (ZIP-based - start with PK)
        MAGIC_BYTES.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[][]{{0x50, 0x4B, 0x03, 0x04}}); // XLSX
        MAGIC_BYTES.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[][]{{0x50, 0x4B, 0x03, 0x04}}); // DOCX
        MAGIC_BYTES.put("application/zip", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}); // ZIP
        MAGIC_BYTES.put("application/x-zip-compressed", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}); // ZIP

        // Old Office formats
        MAGIC_BYTES.put("application/vnd.ms-excel",
                new byte[][]{{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}}); // XLS
    }

    /**
     * Validates an uploaded file for security issues.
     *
     * @param file The uploaded file
     * @throws FileUploadValidationException if validation fails
     */
    public void validate(MultipartFile file) throws FileUploadValidationException {
        if (file == null || file.isEmpty()) {
            throw new FileUploadValidationException("File is empty or null");
        }

        // 1. Check file size
        validateFileSize(file);

        // 2. Sanitize and validate filename
        validateFilename(file.getOriginalFilename());

        // 3. Validate file extension
        validateExtension(file.getOriginalFilename());

        // 4. Validate MIME type (declared)
        validateDeclaredMimeType(file.getContentType());

        // 5. Validate MIME type using magic bytes (actual content)
        try {
            validateMagicBytes(file);
        } catch (IOException e) {
            log.error("Failed to read file content for magic byte validation", e);
            throw new FileUploadValidationException("Failed to validate file content: " + e.getMessage());
        }
    }

    /**
     * Sanitizes a filename to prevent security issues.
     * Removes path traversal attempts and dangerous characters.
     *
     * @param filename Original filename
     * @return Sanitized filename
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_" + System.currentTimeMillis();
        }

        // Remove path components (only keep the filename)
        String sanitized = filename;
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            sanitized = filename.substring(lastSlash + 1);
        }

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        // Remove path traversal attempts
        sanitized = PATH_TRAVERSAL_PATTERN.matcher(sanitized).replaceAll("_");

        // Replace dangerous characters with underscore
        sanitized = DANGEROUS_FILENAME_PATTERN.matcher(sanitized).replaceAll("_");

        // Limit filename length (max 255 chars)
        if (sanitized.length() > 255) {
            String ext = getFileExtension(sanitized);
            int maxNameLength = 255 - ext.length() - 1;
            sanitized = sanitized.substring(0, maxNameLength) + "." + ext;
        }

        // If after sanitization the filename is empty or just an extension, generate a name
        if (sanitized.isEmpty() || sanitized.startsWith(".")) {
            sanitized = "file_" + System.currentTimeMillis() + sanitized;
        }

        return sanitized;
    }

    private void validateFileSize(MultipartFile file) throws FileUploadValidationException {
        if (file.getSize() > maxFileSizeBytes) {
            throw new FileUploadValidationException(
                    String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                            file.getSize(), maxFileSizeBytes));
        }

        if (file.getSize() == 0) {
            throw new FileUploadValidationException("File is empty (0 bytes)");
        }
    }

    private void validateFilename(String filename) throws FileUploadValidationException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileUploadValidationException("Filename is empty");
        }

        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(filename).find()) {
            throw new FileUploadValidationException("Filename contains path traversal characters: " + filename);
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            throw new FileUploadValidationException("Filename contains null bytes");
        }

        // Check filename length
        if (filename.length() > 255) {
            throw new FileUploadValidationException("Filename is too long (max 255 characters)");
        }
    }

    private void validateExtension(String filename) throws FileUploadValidationException {
        if (filename == null) {
            throw new FileUploadValidationException("Filename is null");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (extension.isEmpty()) {
            throw new FileUploadValidationException("File has no extension");
        }

        Set<String> allowedExtensions = parseAllowedExtensions();
        if (!allowedExtensions.contains("." + extension)) {
            throw new FileUploadValidationException(
                    "File extension '." + extension + "' is not allowed. Allowed: " + allowedExtensions);
        }
    }

    private void validateDeclaredMimeType(String contentType) throws FileUploadValidationException {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new FileUploadValidationException("Content-Type header is missing");
        }

        // Extract base MIME type (remove parameters like charset)
        String baseMimeType = contentType.split(";")[0].trim().toLowerCase();

        Set<String> allowedMimeTypes = parseAllowedMimeTypes();
        if (!allowedMimeTypes.contains(baseMimeType)) {
            // Special case: text/plain is often safe to allow even if not explicitly listed
            if (!baseMimeType.equals("text/plain") && !baseMimeType.equals("application/octet-stream")) {
                throw new FileUploadValidationException(
                        "MIME type '" + baseMimeType + "' is not allowed. Allowed: " + allowedMimeTypes);
            }
        }
    }

    private void validateMagicBytes(MultipartFile file) throws IOException, FileUploadValidationException {
        String declaredMimeType = file.getContentType();
        if (declaredMimeType == null) {
            throw new FileUploadValidationException("Content-Type is missing");
        }

        String baseMimeType = declaredMimeType.split(";")[0].trim().toLowerCase();

        // Skip magic byte validation for text files (no reliable magic bytes)
        if (baseMimeType.startsWith("text/") || baseMimeType.equals("application/octet-stream")) {
            return;
        }

        byte[][] expectedMagicBytes = MAGIC_BYTES.get(baseMimeType);
        if (expectedMagicBytes == null) {
            // No magic bytes defined for this type, skip validation
            log.debug("No magic bytes defined for MIME type: {}", baseMimeType);
            return;
        }

        // Read first 16 bytes for magic byte detection
        byte[] fileHeader = new byte[16];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(fileHeader);
            if (bytesRead < 4) {
                throw new FileUploadValidationException("File is too small to validate content type");
            }
        }

        // Check if any of the expected magic byte sequences match
        boolean matches = false;
        for (byte[] magic : expectedMagicBytes) {
            if (matchesMagicBytes(fileHeader, magic)) {
                matches = true;
                break;
            }
        }

        if (!matches) {
            throw new FileUploadValidationException(
                    "File content does not match declared MIME type '" + baseMimeType + "'. " +
                            "Possible file type mismatch or corrupted file.");
        }
    }

    private boolean matchesMagicBytes(byte[] fileHeader, byte[] magicBytes) {
        if (fileHeader.length < magicBytes.length) {
            return false;
        }
        for (int i = 0; i < magicBytes.length; i++) {
            if (fileHeader[i] != magicBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    private Set<String> parseAllowedMimeTypes() {
        if (allowedMimeTypesConfig == null || allowedMimeTypesConfig.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(allowedMimeTypesConfig.split(",")));
    }

    private Set<String> parseAllowedExtensions() {
        if (allowedExtensionsConfig == null || allowedExtensionsConfig.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(allowedExtensionsConfig.split(",")));
    }

    /**
     * Exception thrown when file upload validation fails.
     */
    public static class FileUploadValidationException extends Exception {
        public FileUploadValidationException(String message) {
            super(message);
        }
    }
}
