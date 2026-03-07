package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.service.LookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class LookupControllerTest {

    @Mock
    private LookupService lookupService;

    private LookupController lookupController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        lookupController = new LookupController(lookupService); // Inject mock
    }

    @Test
    public void testGetCountries_ReturnsCountryListSuccessfully() {
        // Arrange
        List<LookupDto> mockCountries = List.of(
                new LookupDto("India"),
                new LookupDto("Germany")
        );
        when(lookupService.getLookupsList("Country", null)).thenReturn(mockCountries);

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getLookups("Country", null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, ((List<LookupDto>) response.getBody().getData()).size());
        assertEquals("India", ((List<LookupDto>) response.getBody().getData()).get(0).getName());
        assertEquals("Germany", ((List<LookupDto>) response.getBody().getData()).get(1).getName());
    }

    @Test
    public void testGetCountries_HandlesEmptyCountryList() {
        // Arrange
        when(lookupService.getLookupsList("Country", null)).thenReturn(List.of());

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getLookups("Country", null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, ((List<LookupDto>) response.getBody().getData()).size());
    }

    @Test
    public void testGetServiceLineLookups_ReturnsServiceLineListSuccessfully() {
        // Arrange
        List<LookupDto> mockServiceLines = List.of(
                new LookupDto("Consulting"),
                new LookupDto("Technology")
        );
        when(lookupService.getServiceLineLookupsList("ServiceLine", null)).thenReturn(mockServiceLines);

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getServiceLineLookups("ServiceLine", null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, ((List<LookupDto>) response.getBody().getData()).size());
        assertEquals("Consulting", ((List<LookupDto>) response.getBody().getData()).get(0).getName());
        assertEquals("Technology", ((List<LookupDto>) response.getBody().getData()).get(1).getName());
    }

    @Test
    public void testGetServiceLineLookups_HandlesEmptyServiceLineList() {
        // Arrange
        when(lookupService.getServiceLineLookupsList("ServiceLine", null)).thenReturn(List.of());

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getServiceLineLookups("ServiceLine", null);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, ((List<LookupDto>) response.getBody().getData()).size());
    }

    @Test
    public void testGetAiVoicePreviewUrl_ReturnsListSuccessfully() {
        // Arrange
        List<AiVoicePreviewLookupDto> mockList = List.of(
                new AiVoicePreviewLookupDto("pk1", "sk1", "file1.mp3", "male", "en-US", "/path1", "voiceId1", "voiceName1", "https://voice1.url"),
                new AiVoicePreviewLookupDto("pk2", "sk2", "file2.mp3", "female", "de-DE", "/path2", "voiceId2", "voiceName2", "https://voice2.url")
        );
        when(lookupService.generateAiVoicePreviewUrls()).thenReturn(mockList);

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getAiVoicePreviewUrl();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        List<AiVoicePreviewLookupDto> data = (List<AiVoicePreviewLookupDto>) response.getBody().getData();
        assertEquals(2, data.size());
        assertEquals("en-US", data.get(0).getLanguage());
        assertEquals("https://voice1.url", data.get(0).getPreSignedUrl());
        assertEquals("https://voice2.url", data.get(1).getPreSignedUrl());
    }

    @Test
    public void testGetAiVoicePreviewUrl_ReturnsEmptyList() {
        // Arrange
        when(lookupService.generateAiVoicePreviewUrls()).thenReturn(List.of());

        // Act
        ResponseEntity<HttpResponse> response = lookupController.getAiVoicePreviewUrl();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        List<AiVoicePreviewLookupDto> data = (List<AiVoicePreviewLookupDto>) response.getBody().getData();
        assertTrue(data.isEmpty());
    }
}
