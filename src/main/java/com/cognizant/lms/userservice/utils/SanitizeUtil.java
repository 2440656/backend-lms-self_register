package com.cognizant.lms.userservice.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Component
@Slf4j
public class SanitizeUtil {
    public static String sanitizeData(String unSanitizedData) {
        if (unSanitizedData == null || unSanitizedData.isEmpty()) {
            return null; // Return null if input is empty or null
        }
        String sanitizedData = unSanitizedData.replaceAll("\\p{M}", "");
        String lowerCased = sanitizedData.toLowerCase();
        if (lowerCased.startsWith("http:") || lowerCased.startsWith("https:") || lowerCased.startsWith("file:")) {
            throw new SecurityException("Potential SSRF detected in input: ");
        }
        return sanitizedData;
    }



    // Canonicalize and validate paths
    public static Path validateAndCanonicalizePath(String baseDir, String inputPath) throws SecurityException {
        try {
            Path basePath = Paths.get(baseDir).toAbsolutePath().normalize(); // Base directory
            Path resolvedPath = basePath.resolve(inputPath).normalize(); // Canonicalize input path

            // Ensure the resolved path is within the base directory
            if (!resolvedPath.startsWith(basePath)) {
                throw new SecurityException("Attempt to access a file outside the allowed directory");
            }

            return resolvedPath;
        } catch (Exception e) {
            throw new SecurityException("Invalid file path", e);
        }
    }

    public static String sanitizeFileName(String fileName) { //sast12Apr
        if (fileName == null) {
            return null;
        }
        String sanitizedData = fileName.replaceAll("\\p{M}", "");
        String lowerCased = sanitizedData.toLowerCase();
        if (lowerCased.startsWith("http:") || lowerCased.startsWith("https:")) {
            throw new SecurityException("Potential SSRF detected in input: ");
        }
        return sanitizedData;
    }

    public static String sanitizePath(String path) { //sast12Apr
        if (path == null) {
            return "";
        }
        String sanitizedData = path.replaceAll("\\p{M}", "");
        String lowerCased = sanitizedData.toLowerCase();
        if (lowerCased.startsWith("http:") || lowerCased.startsWith("https:")) {
            throw new SecurityException("Potential SSRF detected in input: ");
        }
        return sanitizedData;
    }
    public static String sanitizeOriginalData(String unSanitizedData) {
        if (unSanitizedData == null || unSanitizedData.isEmpty()) {
            return null;
        }

        String sanitizedData = unSanitizedData.replaceAll("\\p{M}", "");

        // Check for SSRF patterns without lowercasing
        String check = sanitizedData.trim();
        if (check.startsWith("http:") || check.startsWith("https:") || check.startsWith("file:") ||
            check.toLowerCase().contains("127.0.0.1") || check.toLowerCase().contains("localhost")) {
            throw new SecurityException("Potential SSRF detected in input");
        }

        return sanitizedData;
    }

    /**
     * Parses and clamps the `perPage` input to a safe integer range.
     * - Non-numeric or empty -> defaults to min.
     * - Negative/zero -> min.
     * - Larger than max -> max.
     */
    public static int sanitizePerPage(String perPage, int min, int max) {
        int safeMin = Math.max(1, min); // enforce at least 1
        int safeMax = Math.max(safeMin, max); // ensure max >= min

        if (StringUtils.isBlank(perPage)) {
            log.warn("perPage is blank; defaulting to min {}", safeMin);
            return safeMin;
        }

        // Allow only simple digits to avoid injection or overflow constructs
        if (!perPage.matches("^[0-9]{1,9}$")) {
            log.warn("perPage '{}' is non-numeric or too large; defaulting to min {}", perPage, safeMin);
            return safeMin;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(perPage);
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse perPage '{}'; defaulting to min {}", perPage, safeMin);
            return safeMin;
        }

        if (parsed < safeMin) {
            log.warn("perPage '{}' < min {}; coercing to {}", parsed, safeMin, safeMin);
            return safeMin;
        }
        if (parsed > safeMax) {
            log.warn("perPage '{}' > max {}; coercing to {}", parsed, safeMax, safeMax);
            return safeMax;
        }
        return parsed;
    }
}