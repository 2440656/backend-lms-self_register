package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.BannerManagementDto;
import com.cognizant.lms.userservice.dto.BannerManagementResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.exception.BannerManagementActiveLimitException;
import com.cognizant.lms.userservice.service.BannerManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerManagementControllerTest {

    @InjectMocks
    private BannerManagementController bannerManagementController;

    @Mock
    private BannerManagementService bannerManagementService;

    private BannerManagementDto validBannerDto;
    private MultipartFile validFile;
    private String validBannerDetailsJson;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        validBannerDto = new BannerManagementDto();
        validBannerDto.setBannerId("banner-001");
        validBannerDto.setBannerTitle("Spring Sale");
        validBannerDto.setBannerDescription("Up to 50% off on all items");
        validBannerDto.setBannerStatus("ACTIVE");
        validBannerDto.setStartDate("2024-07-01");
        validBannerDto.setEndDate("2024-07-31");
        validBannerDto.setBannerHeading("Biggest Sale of the Year");
        validBannerDto.setBannerSubHeading("Don't miss out!");
        validBannerDto.setBannerRedirectionUrl("https://example.com/sale");
        validBannerDto.setBannerImageKey("spring_sale_banner.png");

        validFile = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "test image content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        try {
            validBannerDetailsJson = objectMapper.writeValueAsString(validBannerDto);
        } catch (Exception e) {
            validBannerDetailsJson = "{}";
        }
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithFile() {
        // Given
        BannerManagementDto savedBannerDto = new BannerManagementDto();
        savedBannerDto.setBannerId("banner-001");
        savedBannerDto.setBannerTitle("Spring Sale");
        savedBannerDto.setBannerDescription("Up to 50% off on all items");
        savedBannerDto.setBannerStatus("ACTIVE");
        savedBannerDto.setStartDate("2024-07-01");
        savedBannerDto.setEndDate("2024-07-31");
        savedBannerDto.setBannerHeading("Biggest Sale of the Year");
        savedBannerDto.setBannerSubHeading("Don't miss out!");
        savedBannerDto.setBannerRedirectionUrl("https://example.com/sale");
        savedBannerDto.setBannerImageKey("uploaded_image_key.jpg");

        when(bannerManagementService.saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class)))
                .thenReturn(savedBannerDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(validBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED.value(), response.getBody().getStatus());
        assertNull(response.getBody().getError());
        assertNotNull(response.getBody().getData());

        verify(bannerManagementService, times(1))
                .saveBannerManagementIcon(any(BannerManagementDto.class), eq(validFile));
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithoutFile() {
        // Given
        when(bannerManagementService.saveBannerManagementIcon(any(BannerManagementDto.class), isNull()))
                .thenReturn(validBannerDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(validBannerDetailsJson, null);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED.value(), response.getBody().getStatus());
        assertNull(response.getBody().getError());
        assertNotNull(response.getBody().getData());

        verify(bannerManagementService, times(1))
                .saveBannerManagementIcon(any(BannerManagementDto.class), isNull());
    }

    @Test
    void testSaveBannerManagementIcon_InvalidJson() {
        // Given
        String invalidJson = "{invalid json}";

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(invalidJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid banner details format", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingBannerTitle() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerSubHeading("SubHeading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing bannerTitle

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Mandatory fields are missing", response.getBody().getError());
        assertNull(response.getBody().getData());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingBannerDescription() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerSubHeading("SubHeading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing bannerDescription

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingStartDate() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerSubHeading("SubHeading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing startDate

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingEndDate() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerSubHeading("SubHeading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing endDate

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingBannerHeading() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerSubHeading("SubHeading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing bannerHeading

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingBannerSubHeading() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerRedirectionUrl("https://example.com");
        // Missing bannerSubHeading

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_MissingBannerRedirectionUrl() throws Exception {
        // Given
        BannerManagementDto incompleteDto = new BannerManagementDto();
        incompleteDto.setBannerTitle("Title");
        incompleteDto.setBannerDescription("Description");
        incompleteDto.setStartDate("2024-07-01");
        incompleteDto.setEndDate("2024-07-31");
        incompleteDto.setBannerHeading("Heading");
        incompleteDto.setBannerSubHeading("SubHeading");
        // Missing bannerRedirectionUrl

        String incompleteBannerDetailsJson = objectMapper.writeValueAsString(incompleteDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(incompleteBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_BannerManagementActiveLimitException() {
        // Given
        String errorMessage = "Maximum number of active banners reached";
        when(bannerManagementService.saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class)))
                .thenThrow(new BannerManagementActiveLimitException(errorMessage));

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(validBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getError());
        assertNull(response.getBody().getData());

        verify(bannerManagementService, times(1))
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_RuntimeException() {
        // Given
        String errorMessage = "Database connection failed";
        when(bannerManagementService.saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(validBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertTrue(response.getBody().getError().contains("An error occurred while saving banner details"));
        assertTrue(response.getBody().getError().contains(errorMessage));
        assertNull(response.getBody().getData());

        verify(bannerManagementService, times(1))
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_RuntimeExceptionWithNullMessage() {
        // Given
        when(bannerManagementService.saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class)))
                .thenThrow(new RuntimeException());

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(validBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertTrue(response.getBody().getError().contains("An error occurred while saving banner details"));
        assertTrue(response.getBody().getError().contains("internal error"));
        assertNull(response.getBody().getData());

        verify(bannerManagementService, times(1))
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testSaveBannerManagementIcon_AllFieldsEmpty() throws Exception {
        // Given
        BannerManagementDto emptyDto = new BannerManagementDto();
        // All fields are null/empty
        String emptyBannerDetailsJson = objectMapper.writeValueAsString(emptyDto);

        // When
        ResponseEntity<HttpResponse> response = bannerManagementController
                .saveBannerManagementIcon(emptyBannerDetailsJson, validFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Mandatory fields are missing", response.getBody().getError());

        verify(bannerManagementService, never())
                .saveBannerManagementIcon(any(BannerManagementDto.class), any(MultipartFile.class));
    }

    @Test
    void testConstructor() {
        // Given
        BannerManagementService service = mock(BannerManagementService.class);

        // When
        BannerManagementController controller = new BannerManagementController(service);

        // Then
        assertNotNull(controller);
    }

    @Test
    void testGetAllBanners_Success() {
        BannerManagementResponse serviceResponse = new BannerManagementResponse();
        when(bannerManagementService.getAllBanners()).thenReturn(serviceResponse);

        ResponseEntity<HttpResponse> response = bannerManagementController.getAllBanners();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertNull(body.getError());
        assertSame(serviceResponse, body.getData());

        verify(bannerManagementService, times(1)).getAllBanners();
    }

    @Test
    void testGetAllBanners_RuntimeException() {
        String errorMessage = "Database error";
        when(bannerManagementService.getAllBanners()).thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<HttpResponse> response = bannerManagementController.getAllBanners();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Failed to fetch Banners", body.getError());
        assertNull(body.getData());

        verify(bannerManagementService, times(1)).getAllBanners();
    }
}
