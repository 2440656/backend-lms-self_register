package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileUtilTest {

    @Test
    public void testSaveFileToLocal_IOException() throws IOException {
        // Arrange
        FileUtil fileUtil = new FileUtil();
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "csv content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Test IOException");
            }
        };
        String localStoragePath = "local/storage/path/";
        String fileStorageSuffix = "suffix/";

        // Act
        boolean result = fileUtil.saveFileToLocal(file, localStoragePath, fileStorageSuffix);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testValidateFile_ValidCSVFile() {
        // Arrange
        FileUtil fileUtil = new FileUtil();
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "csv content".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertDoesNotThrow(() -> fileUtil.validateFile(file));
    }

    @Test
    public void testValidateFile_InvalidFileFormat() {
        // Arrange
        FileUtil fileUtil = new FileUtil();
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "text content".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> fileUtil.validateFile(file));
        assertEquals("Invalid file format, Only CSV files are allowed", exception.getMessage());
    }

    @Test
    public void testValidateFile_FileSizeExceedsLimit() {
        // Arrange
        FileUtil fileUtil = new FileUtil();
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[(int) (Constants.MAX_FILE_SIZE + 1)]);

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> fileUtil.validateFile(file));
        assertEquals("File size exceeds the limit", exception.getMessage());
    }

}
