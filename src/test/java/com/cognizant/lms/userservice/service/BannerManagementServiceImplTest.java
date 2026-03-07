package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.BannerManagementDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.BannerManagementDto;
import com.cognizant.lms.userservice.dto.BannerManagementResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;

import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Test class for BannerManagementServiceImpl.
 * Tests cover:
 * - Banner creation with and without files
 * - Banner updates with existing IDs
 * - File validation (size, type, dimensions)
 * - S3 file upload functionality
 * - Error handling for various scenarios
 * Note: Banner limit functionality has been removed from the service implementation.
 */
@ExtendWith(MockitoExtension.class)
class BannerManagementServiceImplTest {

    @Mock
    private BannerManagementDao bannerManagementDao;

    @Mock
    private S3Client s3ThumbnailClient;

    @InjectMocks
    private BannerManagementServiceImpl bannerManagementService;

    private BannerManagementDto validBannerDto;
    private MultipartFile validImageFile;
    private final String tenantCode = "t-123";
    private final String username = "testUser";

    @BeforeEach
    void setUp() {
        // Set up test data for banner management
        validBannerDto = new BannerManagementDto();
        validBannerDto.setBannerTitle("Test Banner");
        validBannerDto.setBannerDescription("Test Description");
        validBannerDto.setBannerStatus("ACTIVE");
        validBannerDto.setStartDate("2024-01-01"); // Required for gsi_banner_type (pk=pk, sk=startDate)
        validBannerDto.setEndDate("2024-12-31");
        validBannerDto.setBannerHeading("Test Heading");
        validBannerDto.setBannerSubHeading("Test Sub Heading");
        validBannerDto.setBannerRedirectionUrl("https://example.com");

        // Create valid image file (577x686 pixels)
        validImageFile = createValidImageFile();

        // Set up bucket name using ReflectionTestUtils
        ReflectionTestUtils.setField(bannerManagementService, "bucketName", "test-bucket");

        // Set up TenantUtil
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk(tenantCode);
        TenantUtil.setTenantDetails(tenantDTO);
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithNewBannerAndFile() {
        // Given
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, validImageFile);

            // Then
            assertNotNull(result, "Result should not be null");
            assertNotNull(result.getBannerId(), "BannerId should not be null");
            assertTrue(result.getBannerId().startsWith("BANNER#"),
                      "BannerId should start with BANNER#");
            assertEquals(validBannerDto.getBannerTitle(), result.getBannerTitle());
            assertEquals(validBannerDto.getBannerDescription(), result.getBannerDescription());
            assertNotNull(result.getBannerImageKey());
            assertTrue(result.getBannerImageKey().contains("banners/image/" + tenantCode));

            verify(s3ThumbnailClient, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithExistingBannerId() {
        // Given
        String existingBannerId = "BANNER#existing-123";
        validBannerDto.setBannerId(existingBannerId);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, validImageFile);

            // Then
            assertNotNull(result);
            assertEquals(existingBannerId, result.getBannerId());

            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(
                argThat(tenant -> existingBannerId.equals(tenant.getBannerId()))
            );
        }
    }

    @Test
    void testSaveBannerManagementIcon_EndDateProvided_IsPreserved() {
        // Given
        validBannerDto.setEndDate("2025-12-31");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(
                    argThat(tenant -> "2025-12-31".equals(tenant.getEndDate()))
            );
        }
    }

    @Test
    void testSaveBannerManagementIcon_EndDateEmptyString_BecomesNull() {
        // Given
        validBannerDto.setEndDate("");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(
                    argThat(tenant -> tenant.getEndDate() == null)
            );
        }
    }

    @Test
    void testSaveBannerManagementIcon_EndDateNull_BecomesNull() {
        // Given
        validBannerDto.setEndDate(null);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(
                    argThat(tenant -> tenant.getEndDate() == null)
            );
        }
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithoutFile() {
        // Given

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then
            assertNotNull(result);
            assertNotNull(result.getBannerId());
            assertTrue(result.getBannerId().startsWith("BANNER#"));
            assertNull(result.getBannerImageKey());

            verify(s3ThumbnailClient, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testSaveBannerManagementIcon_Success_WithEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, emptyFile);

            // Then
            assertNotNull(result);
            assertNotNull(result.getBannerId());
            assertNull(result.getBannerImageKey());

            verify(s3ThumbnailClient, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testValidateFile_FileSizeExceeded() {
        // Given
        byte[] largeFileContent = new byte[4 * 1024 * 1024]; // 4MB file
        MultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeFileContent);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, largeFile));

            // Service now enforces 1MB limit
            assertTrue(exception.getMessage().contains("1MB") || exception.getMessage().contains("less than or equal to 1MB"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testValidateFile_InvalidContentType() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, invalidFile));

            assertTrue(exception.getMessage().contains("Invalid file type"));
            assertTrue(exception.getMessage().contains("text/plain"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testValidateFile_InvalidImageDimensions() {
        // Given - Create image with wrong dimensions (100x100 instead of 577x686)
        MultipartFile invalidDimensionsFile = createImageWithDimensions(100, 100);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, invalidDimensionsFile));

            // Service now requires minimum dimensions 500x500
            assertTrue(exception.getMessage().contains("at least 500x500") || exception.getMessage().contains("500x500"));
            assertTrue(exception.getMessage().contains("Current dimensions: 100x100"));
             verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testValidateFile_CorruptedImageFile() {
        // Given
        MultipartFile corruptedFile = new MockMultipartFile("file", "corrupted.jpg", "image/jpeg",
            "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, corruptedFile));

            assertTrue(exception.getMessage().contains("Invalid image file"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testValidateFile_NullFile() {
        // Given
        MultipartFile nullFile = null;

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then - Should not throw exception as null file is handled gracefully
            assertDoesNotThrow(() -> bannerManagementService.saveBannerManagementIcon(validBannerDto, nullFile));
        }
    }

    @Test
    void testValidateFile_EmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then - Empty file should be handled gracefully
            assertDoesNotThrow(() -> bannerManagementService.saveBannerManagementIcon(validBannerDto, emptyFile));
        }
    }

    @Test
    void testSaveBannerManagementIcon_S3UploadFailure() {
        // Given
        when(s3ThumbnailClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(new RuntimeException("S3 upload failed"));

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, validImageFile));

            assertTrue(exception.getMessage().contains("Failed to upload file to S3") ||
                      exception.getMessage().contains("S3 upload failed"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testSaveBannerManagementIcon_FileWithoutContentType() {
        // Given
        MultipartFile fileWithoutContentType = new MockMultipartFile("file", "test.jpg", null,
            createValidImageBytes());

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then - This should fail validation because null content type is not in allowed types
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, fileWithoutContentType));

            assertTrue(exception.getMessage().contains("Invalid file type"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testToEntity_Conversion() {
        // Given
        validBannerDto.setBannerId("BANNER#test-123");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then - Verify entity creation by checking the DAO call
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(
                argThat(tenant -> tenantCode.equals(tenant.getPk()) &&
                           "BANNER#test-123".equals(tenant.getSk()) &&
                           validBannerDto.getBannerTitle().equals(tenant.getBannerTitle()) &&
                           validBannerDto.getBannerDescription().equals(tenant.getBannerDescription()) &&
                           validBannerDto.getBannerStatus().equals(tenant.getBannerStatus()) &&
                           username.equals(tenant.getCreatedBy()) &&
                           tenant.getCreatedOn() != null)
            );
        }
    }

    @Test
    void testToDto_Conversion() {
        // Given
        Tenant tenant = new Tenant();
        tenant.setBannerId("BANNER#test-456");
        tenant.setBannerTitle("Test Title");
        tenant.setBannerDescription("Test Description");
        tenant.setBannerStatus("ACTIVE");
        tenant.setStartDate("2024-01-01");
        tenant.setEndDate("2024-12-31");
        tenant.setBannerHeading("Test Heading");
        tenant.setBannerSubHeading("Test Sub Heading");
        tenant.setBannerRedirectionUrl("https://example.com");
        tenant.setBannerImageKey("test-image-key");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // Mock DAO to return our test tenant
            doAnswer(invocation -> {
                Tenant arg = invocation.getArgument(0);
                // Copy test tenant data to the argument
                arg.setBannerId(tenant.getBannerId());
                arg.setBannerTitle(tenant.getBannerTitle());
                arg.setBannerDescription(tenant.getBannerDescription());
                arg.setBannerStatus(tenant.getBannerStatus());
                arg.setStartDate(tenant.getStartDate());
                arg.setEndDate(tenant.getEndDate());
                arg.setBannerHeading(tenant.getBannerHeading());
                arg.setBannerSubHeading(tenant.getBannerSubHeading());
                arg.setBannerRedirectionUrl(tenant.getBannerRedirectionUrl());
                arg.setBannerImageKey(tenant.getBannerImageKey());
                return null;
            }).when(bannerManagementDao).saveBannerManagementIcon(any(Tenant.class));

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then
            assertNotNull(result);
            assertEquals(tenant.getBannerTitle(), result.getBannerTitle());
            assertEquals(tenant.getBannerDescription(), result.getBannerDescription());
            assertEquals(tenant.getBannerStatus(), result.getBannerStatus());
            assertEquals(tenant.getStartDate(), result.getStartDate());
            assertEquals(tenant.getEndDate(), result.getEndDate());
            assertEquals(tenant.getBannerHeading(), result.getBannerHeading());
            assertEquals(tenant.getBannerSubHeading(), result.getBannerSubHeading());
            assertEquals(tenant.getBannerRedirectionUrl(), result.getBannerRedirectionUrl());
        }
    }

    @Test
    void testConstructor() {
        // Given
        BannerManagementDao dao = mock(BannerManagementDao.class);
        S3Client s3Client = mock(S3Client.class);

        // When
        BannerManagementServiceImpl service = new BannerManagementServiceImpl(dao, s3Client);

        // Then
        assertNotNull(service);
    }

    @Test
    void testBannerIdGenerationSimple() {
        // Given - Simple test with minimal mocking
        BannerManagementDto dto = new BannerManagementDto();
        dto.setBannerTitle("Test");
        dto.setBannerDescription("Test Desc");
        dto.setBannerStatus("INACTIVE"); // Use INACTIVE to skip active limit check
        dto.setStartDate("2024-01-01");
        dto.setEndDate("2024-12-31");
        dto.setBannerHeading("Heading");
        dto.setBannerSubHeading("SubHeading");
        dto.setBannerRedirectionUrl("https://example.com");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn("testUser");
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-123");

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(dto, null);

            // Then
            assertNotNull(result, "Result should not be null");
            assertNotNull(result.getBannerId(), "BannerId should not be null");

            // Debug output to see what we actually get
            System.out.println("Generated bannerId: '" + result.getBannerId() + "'");
            System.out.println("BannerId length: " + (result.getBannerId() != null ? result.getBannerId().length() : "null"));
            System.out.println("Starts with BANNER#: " + (result.getBannerId() != null && result.getBannerId().startsWith("BANNER#")));

            // The main assertion
            if (result.getBannerId() != null && !result.getBannerId().startsWith("BANNER#")) {
                fail("BannerId should start with BANNER# but was: '" + result.getBannerId() + "'");
            }

            assertTrue(result.getBannerId() != null && result.getBannerId().startsWith("BANNER#"),
                      "BannerId should start with BANNER#: '" + result.getBannerId() + "'");
        }
    }

    @Test
    void testValidateFile_AllowedContentTypes() {
        // Test all allowed content types
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png"};

        for (String contentType : allowedTypes) {
            MultipartFile file = new MockMultipartFile("file", "test." + contentType.split("/")[1],
                contentType, createValidImageBytes());

            try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
                 MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

                userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
                tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

                // Should not throw exception
                assertDoesNotThrow(() -> bannerManagementService.saveBannerManagementIcon(validBannerDto, file));
            }
        }
    }

    @Test
    void testSaveBannerManagementIcon_EmptyBannerId() {
        // Given
        validBannerDto.setBannerId(""); // Empty banner ID

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When
            BannerManagementDto result = bannerManagementService.saveBannerManagementIcon(validBannerDto, null);

            // Then - Should generate new banner ID
            assertNotNull(result);
            assertNotNull(result.getBannerId());
            assertTrue(result.getBannerId().startsWith("BANNER#"));
            assertNotEquals("", result.getBannerId());
        }
    }

    @Test
    void testSaveBannerManagementIcon_DaoException() {
        // Given
        doThrow(new RuntimeException("Database error")).when(bannerManagementDao).saveBannerManagementIcon(any(Tenant.class));

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, null));

            assertEquals("Database error", exception.getMessage());
            verify(bannerManagementDao, times(1)).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    @Test
    void testSaveBannerManagementIcon_FileValidationIOException() {
        // Given - Create a file that will cause IOException when reading
        MultipartFile problematicFile = mock(MultipartFile.class);
        when(problematicFile.isEmpty()).thenReturn(false);
        when(problematicFile.getSize()).thenReturn(1024L);
        when(problematicFile.getContentType()).thenReturn("image/jpeg");
        try {
            when(problematicFile.getInputStream()).thenThrow(new IOException("Stream error"));
        } catch (IOException e) {
            // This won't happen in test
        }

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

            userContextMock.when(UserContext::getCreatedBy).thenReturn(username);
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bannerManagementService.saveBannerManagementIcon(validBannerDto, problematicFile));

            assertTrue(exception.getMessage().contains("Error validating image dimensions"));
            verify(bannerManagementDao, never()).saveBannerManagementIcon(any(Tenant.class));
        }
    }

    // Helper methods
    private MultipartFile createValidImageFile() {
        return createImageWithDimensions(577, 686);
    }

    private MultipartFile createImageWithDimensions(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            return new MockMultipartFile("file", "test.jpg", "image/jpeg", imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test image", e);
        }
    }

    private byte[] createValidImageBytes() {
        try {
            BufferedImage image = new BufferedImage(577, 686, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test image bytes", e);
        }
    }

    @Test
    void testGetAllBanners_Success() {
        String tenantCode = "t-123";
        Tenant t1 = new Tenant();
        t1.setBannerId("BANNER#1");
        t1.setBannerTitle("Title 1");
        t1.setBannerDescription("Desc 1");
        t1.setBannerStatus("ACTIVE");
        t1.setStartDate("2024-01-01");
        t1.setEndDate("2024-12-31");
        t1.setBannerHeading("Heading 1");
        t1.setBannerSubHeading("Sub 1");
        t1.setBannerRedirectionUrl("https://example.com/1");
        t1.setBannerImageKey("img/key/1");

        Tenant t2 = new Tenant();
        t2.setBannerId("BANNER#2");
        t2.setBannerTitle("Title 2");
        t2.setBannerDescription("Desc 2");
        t2.setBannerStatus("INACTIVE");
        t2.setStartDate("2024-02-01");
        t2.setEndDate("2024-11-30");
        t2.setBannerHeading("Heading 2");
        t2.setBannerSubHeading("Sub 2");
        t2.setBannerRedirectionUrl("https://example.com/2");
        t2.setBannerImageKey("img/key/2");

        List<Tenant> tenants = List.of(t1, t2);
        when(bannerManagementDao.getBannersByTenant(tenantCode)).thenReturn(tenants);

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            BannerManagementResponse response = bannerManagementService.getAllBanners();

            assertNotNull(response);
            assertEquals(2, response.getTotalActiveRecords());
            assertNotNull(response.getBannerManagementList());
            assertEquals(2, response.getBannerManagementList().size());

            BannerManagementDto dto1 = response.getBannerManagementList().get(0);
            BannerManagementDto dto2 = response.getBannerManagementList().get(1);

            assertEquals(t1.getBannerId(), dto1.getBannerId());
            assertEquals(t1.getBannerTitle(), dto1.getBannerTitle());
            assertEquals(t1.getBannerImageKey(), dto1.getBannerImageKey());

            assertEquals(t2.getBannerId(), dto2.getBannerId());
            assertEquals(t2.getBannerRedirectionUrl(), dto2.getBannerRedirectionUrl());

            verify(bannerManagementDao, times(1)).getBannersByTenant(tenantCode);
        }
    }

    @Test
    void testGetAllBanners_EmptyList() {
        String tenantCode = "t-123";
        when(bannerManagementDao.getBannersByTenant(tenantCode)).thenReturn(List.of());

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn(tenantCode);

            BannerManagementResponse response = bannerManagementService.getAllBanners();

            assertNotNull(response);
            assertEquals(0, response.getTotalActiveRecords());
            assertNotNull(response.getBannerManagementList());
            assertTrue(response.getBannerManagementList().isEmpty());

            verify(bannerManagementDao, times(1)).getBannersByTenant(tenantCode);
        }
    }
}
