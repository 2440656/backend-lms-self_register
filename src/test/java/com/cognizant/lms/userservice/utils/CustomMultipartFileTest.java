package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomMultipartFileTest {
    @Test
    void getName_ShouldReturnFileName() throws IOException {
        MultipartFile file = new CustomMultipartFile(Arrays.asList("line1", "line2"), "testFile.txt");
        assertEquals("testFile.txt", file.getName());
        assertEquals("testFile.txt", file.getOriginalFilename());

        assertEquals("text/plain", file.getContentType());

    }

    @Test
    void transferTo_ShouldWriteToFile() throws IOException {
        MultipartFile file = new CustomMultipartFile(Arrays.asList("line1", "line2"), "testFile.txt");
        File dest = Files.createTempFile("testFile", ".txt").toFile();
        file.transferTo(dest);
        assertTrue(dest.exists());
    }
    @Test
    void testCustomMultipartFile() throws IOException {
        List<String> stringList = Arrays.asList("line1", "line2", "line3");
        String fileName = "testFile.txt";
        MultipartFile multipartFile = new CustomMultipartFile(stringList, fileName);

        assertEquals(fileName, multipartFile.getName());
        assertEquals(fileName, multipartFile.getOriginalFilename());
        assertEquals("text/plain", multipartFile.getContentType());
        assertFalse(multipartFile.isEmpty());
        assertEquals(("line1" + System.lineSeparator() + "line2" + System.lineSeparator() + "line3" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8).length, multipartFile.getSize());
        assertArrayEquals(("line1" + System.lineSeparator() + "line2" + System.lineSeparator() + "line3" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), multipartFile.getBytes());

        File tempFile = Files.createTempFile("testFile", ".txt").toFile();
        multipartFile.transferTo(tempFile);
        assertTrue(tempFile.exists());
        assertArrayEquals(("line1" + System.lineSeparator() + "line2" + System.lineSeparator() + "line3" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), Files.readAllBytes(tempFile.toPath()));
        tempFile.deleteOnExit();
    }

    @Test
    void testEmptyCustomMultipartFile() throws IOException {
        List<String> stringList = Arrays.asList();
        String fileName = "emptyFile.txt";
        MultipartFile multipartFile = new CustomMultipartFile(stringList, fileName);

        assertEquals(fileName, multipartFile.getName());
        assertEquals(fileName, multipartFile.getOriginalFilename());
        assertEquals("text/plain", multipartFile.getContentType());
        assertTrue(multipartFile.isEmpty());
        assertEquals(0, multipartFile.getSize());
        assertArrayEquals(new byte[0], multipartFile.getBytes());
    }
}
