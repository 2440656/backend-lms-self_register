package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.exception.FileValidationException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FileUtil {

  public boolean saveFileToLocal(MultipartFile file, String localStoragePath, //sast12Apr
                                 String fileStorageSuffix) {
    try {
      String sanitizedStoragePath = SanitizeUtil.sanitizePath(localStoragePath);
      String sanitizedSuffix = SanitizeUtil.sanitizePath(fileStorageSuffix);
      String sanitizedFileName = SanitizeUtil.sanitizeFileName(file.getName());

      if (sanitizedFileName == null || sanitizedFileName.isEmpty()) {
        throw new IllegalArgumentException("Invalid file name.");
      }

      Path basePath = Paths.get(localStoragePath).toAbsolutePath().normalize();
      Path directoryPath = SanitizeUtil.validateAndCanonicalizePath(basePath.toString(), sanitizedStoragePath + sanitizedSuffix);

      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
        log.info("Directory created: {}", directoryPath);
      }

      Path filePath = directoryPath.resolve(sanitizedFileName).normalize();

      if (!filePath.startsWith(basePath)) {
        throw new SecurityException("Attempt to write a file outside the allowed directory.");
      }

      FileSystemResource fileResource = new FileSystemResource(filePath.toFile());

      try (InputStream inputStream = file.getInputStream();
           OutputStream outputStream = new FileOutputStream(fileResource.getFile())) {
        inputStream.transferTo(outputStream); // Copy content
      }

      log.info("File saved to local storage: {}", filePath);
      return fileResource.exists();

    } catch (SecurityException se) {
      log.error("Security violation: {}", se.getMessage());
      return false;
    } catch (IOException e) {
      log.error("Failed to save file to local storage: {}", e.getMessage());
      return false;
    }
  }

  public void validateFile(MultipartFile file) {
    String originalFileName = file.getOriginalFilename();
    if (originalFileName == null || !isCSVFile(originalFileName)) {
      log.error("Invalid file format, Only CSV files are allowed");
      throw new FileValidationException("Invalid file format, Only CSV files are allowed");
    }
    if (file.getSize() > Constants.MAX_FILE_SIZE) {
      log.error("File size exceeds the limit");
      throw new FileValidationException("File size exceeds the limit");
    }
  }

  private boolean isCSVFile(String fileName) {
    return fileName.endsWith(Constants.fileFormat);
  }
}
