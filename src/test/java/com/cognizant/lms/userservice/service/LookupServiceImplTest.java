package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.S3config;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import org.apache.logging.log4j.core.config.plugins.validation.Constraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

public class LookupServiceImplTest {


    @Mock
    private LookupDao lookupDao;

    @Mock
    private S3config s3config;

    private LookupServiceImpl lookupServiceImpl;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        lookupServiceImpl = new LookupServiceImpl(
                lookupDao,
                s3config, // Mock or provide an instance of S3config
                "mockBucketName",
                "dev"// Mock value for the bucket name
        );
    }

    @Test
    public void testGetLookupsList_ReturnsCountryList() {
        // Arrange
        List<LookupDto> mockCountries = List.of(
                new LookupDto("India"),
                new LookupDto("Germany")
        );
        when(lookupDao.getLookupData("Country", null)).thenReturn(mockCountries);

        // Act
        List<LookupDto> lookupData = lookupServiceImpl.getLookupsList("Country", null);

        // Assert
        assertEquals(2, lookupData.size());
        assertEquals("India", lookupData.get(0).getName());
        assertEquals("Germany", lookupData.get(1).getName());
    }

    @Test
    public void testGetLookupsList_ThrowsRuntimeException() {
        // Arrange
        when(lookupDao.getLookupData("Country", null)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            lookupServiceImpl.getLookupsList("Country", null);
        });
        assertEquals("Database error", exception.getMessage());
    }

    @Test
    public void testGetServiceLineLookupsList_ReturnsServiceLineList() {
        // Arrange
        List<LookupDto> mockServiceLines = List.of(
                new LookupDto("Consulting"),
                new LookupDto("Technology")
        );
        when(lookupDao.getServiceLineLookupData("ServiceLine", null)).thenReturn(mockServiceLines);

        // Act
        List<LookupDto> result = lookupServiceImpl.getServiceLineLookupsList("ServiceLine", null);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Consulting", result.get(0).getName());
        assertEquals("Technology", result.get(1).getName());
    }

    @Test
    public void testGetServiceLineLookupsList_ThrowsRuntimeException() {
        // Arrange
        when(lookupDao.getServiceLineLookupData("ServiceLine", null)).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            lookupServiceImpl.getServiceLineLookupsList("ServiceLine", null);
        });
        assertEquals("DB error", exception.getMessage());
    }

    @Test
    public void testGenerateAiVoicePreviewUrls_EmptyList() {
        // Arrange
        when(lookupDao.getAiVoicePreviewData()).thenReturn(List.of());

        // Mock S3Presigner (not strictly needed for empty list, but ensures consistency)
        S3Presigner presigner = mock(S3Presigner.class);
        try {
            Field presignerField = LookupServiceImpl.class.getDeclaredField("s3Presigner");
            presignerField.setAccessible(true);
            presignerField.set(lookupServiceImpl, presigner);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set s3Presigner field", e);
        }

        // Act
        List<AiVoicePreviewLookupDto> result = lookupServiceImpl.generateAiVoicePreviewUrls();

        // Assert
        assertTrue(result.isEmpty());
    }

}
