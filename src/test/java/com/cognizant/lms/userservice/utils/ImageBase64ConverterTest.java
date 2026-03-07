package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageBase64ConverterTest {
        @Test
        public void testConvertImageToBase64() throws IOException {
            byte[] imageBytes = "test image".getBytes(StandardCharsets.UTF_8);
            String expectedBase64 = Base64.getEncoder().encodeToString(imageBytes);
            String actualBase64 = ImageBase64Converter.convertImageToBase64(imageBytes);
            assertEquals(expectedBase64, actualBase64);
        }

}
