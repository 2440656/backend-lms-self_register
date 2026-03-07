package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.PopularLinkDao;
import com.cognizant.lms.userservice.domain.*;
import com.cognizant.lms.userservice.dto.PopularLinkDto;
import com.cognizant.lms.userservice.dto.PopularLinkRequestDto;
import com.cognizant.lms.userservice.exception.PopularLinkLimitException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PopularLinkServiceImplTest {
    private final PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
    private final S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
    PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

    @Test
    void savePopularLink_success() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");
            Mockito.doNothing().when(dao).savePopularLink(any());
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setTitle("title");
            dto.setUrl("url");
            dto.setDescription("desc");
            PopularLinkDto result = service.savePopularLink(dto, null);
            assertEquals("title", result.getTitle());
            assertEquals("url", result.getUrl());
            assertEquals("desc", result.getDescription());
        }
    }

    @Test
    void savePopularLink_limitReached() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");
            // Return a list of MAX_POPULAR_LINKS dummy tenants
            List<Tenant> twentyTenants = new ArrayList<>();
            for (int i = 0; i < Constants.MAX_POPULAR_LINKS; i++) {
                Tenant tenant = new Tenant();
                tenant.setPk("tenant");
                tenant.setLinkId("id" + i);
                twentyTenants.add(tenant);
            }
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(twentyTenants);
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setTitle("title");
            dto.setUrl("url");
            assertThrows(PopularLinkLimitException.class, () -> service.savePopularLink(dto, null));
        }
    }

    @Test
    void getAllPopularLinks_success() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            Tenant tenant = new Tenant();
            tenant.setLinkId("id");
            tenant.setTitle("title");
            tenant.setUrl("url");
            tenant.setDescription("desc");
            tenant.setIndex(1);
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(java.util.List.of(tenant));
            var resp = service.getAllPopularLinks();
            assertEquals(1, resp.getCount());
            PopularLinkDto firstLink = resp.getPopularLinkList().getFirst();
            assertEquals("id", firstLink.getLinkId());
            assertEquals("title", firstLink.getTitle());
            assertEquals("url", firstLink.getUrl());
            assertEquals("desc", firstLink.getDescription());
        }
    }

    @Test
    void updatePopularLink_missingLinkId() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        PopularLinkRequestDto dto = new PopularLinkRequestDto();
        assertThrows(IllegalArgumentException.class, () -> service.updatePopularLink(dto, null, false, false));
    }

    @Test
    void updatePopularLink_notFound() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = Mockito.mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id");
            Mockito.when(dao.getPopularLinkByPk(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            assertThrows(com.cognizant.lms.userservice.exception.PopularLinkNotFoundException.class,
                () -> service.updatePopularLink(dto, null, false, false));
        }
    }

    @Test
    void updatePopularLink_success_partialUpdate() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao , s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id");
            dto.setTitle("newTitle");
            Tenant existing = new Tenant();
            existing.setPk("id");
            existing.setName("oldTitle");
            Mockito.when(dao.getPopularLinkByPk(Mockito.anyString(), Mockito.anyString())).thenReturn(existing);
            Mockito.when(dao.updatePopularLink(Mockito.any())).thenReturn(existing);
            PopularLinkDto result = service.updatePopularLink(dto, null, true, false);
            assertEquals("newTitle", result.getTitle());
        }
    }

    @Test
    void deletePopularLink_missingLinkId() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao , s3ThumbnailClient);
        assertThrows(IllegalArgumentException.class, () -> service.deletePopularLink(""));
    }

    @Test
    void deletePopularLink_notFound() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            Mockito.when(dao.getPopularLinkByPk(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            assertThrows(com.cognizant.lms.userservice.exception.PopularLinkNotFoundException.class, () -> service.deletePopularLink("id"));
        }
    }

    @Test
    void deletePopularLink_success() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            Tenant tenant = new Tenant();
            tenant.setLinkId("id");
            tenant.setIndex(1);
            Mockito.when(dao.getPopularLinkByPk(Mockito.anyString(), Mockito.anyString())).thenReturn(tenant);
            Mockito.doNothing().when(dao).deletePopularLink(Mockito.anyString(), Mockito.anyString());
            String result = service.deletePopularLink("id");
            assertEquals("id", result);
        }
    }

    @Test
    void reorderPopularLink_nullRequest_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        assertThrows(IllegalArgumentException.class, () -> service.reorderPopularLink(null));
    }

    @Test
    void reorderPopularLink_nullLinkId_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        PopularLinkRequestDto dto = new PopularLinkRequestDto();
        dto.setIndex(2);
        assertThrows(IllegalArgumentException.class, () -> service.reorderPopularLink(dto));
    }

    @Test
    void reorderPopularLink_nullIndex_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        PopularLinkRequestDto dto = new PopularLinkRequestDto();
        dto.setLinkId("id1");
        assertThrows(IllegalArgumentException.class, () -> service.reorderPopularLink(dto));
    }

    @Test
    void reorderPopularLink_linkNotFound_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(new ArrayList<>());
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id1");
            dto.setIndex(2);
            assertThrows(com.cognizant.lms.userservice.exception.PopularLinkNotFoundException.class, () -> service.reorderPopularLink(dto));
        }
    }

    @Test
    void reorderPopularLink_indexOutOfBoundsLow_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            // Setup a list of 3 tenants with proper linkId, sk, and index
            List<Tenant> tenants = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Tenant t = new Tenant();
                t.setLinkId("id" + i);
                t.setSk("POPULARLINK#id" + i);
                t.setIndex(i);
                tenants.add(t);
            }
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(tenants);
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id1");
            dto.setIndex(0); // 0 is out of bounds (assuming 1-based index)
            assertThrows(IllegalArgumentException.class, () -> service.reorderPopularLink(dto));
        }
    }

    @Test
    void reorderPopularLink_indexOutOfBoundsHigh_throwsException() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            // Setup a list of 3 tenants with proper linkId, sk, and index
            List<Tenant> tenants = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Tenant t = new Tenant();
                t.setLinkId("id" + i);
                t.setSk("POPULARLINK#id" + i);
                t.setIndex(i);
                tenants.add(t);
            }
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(tenants);
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id1");
            dto.setIndex(4); // 4 is out of bounds for a list of size 3
            assertThrows(IllegalArgumentException.class, () -> service.reorderPopularLink(dto));
        }
    }

    @Test
    void reorderPopularLink_noChange_returnsNull() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            List<Tenant> tenants = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Tenant t = new Tenant();
                t.setLinkId("id" + i);
                t.setSk("POPULARLINK#id" + i);
                t.setIndex(i);
                tenants.add(t);
            }
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(tenants);
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id2");
            dto.setIndex(2); // No change, already at index 2
            var resp = service.reorderPopularLink(dto);
            assertNull(resp); // Simplified assertion
        }
    }

    @Test
    void reorderPopularLink_moveUp_success() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            // Initial order: id1 (1), id2 (2), id3 (3)
            List<Tenant> tenants = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Tenant t = new Tenant();
                t.setLinkId("id" + i);
                t.setSk("POPULARLINK#id" + i);
                t.setIndex(i);
                tenants.add(t);
            }
            // After moving id3 to index 1: id3 (1), id1 (2), id2 (3)
            List<Tenant> reordered = new ArrayList<>();
            Tenant t3 = new Tenant(); t3.setLinkId("id3"); t3.setSk("POPULARLINK#id3"); t3.setIndex(1); reordered.add(t3);
            Tenant t1 = new Tenant(); t1.setLinkId("id1"); t1.setSk("POPULARLINK#id1"); t1.setIndex(2); reordered.add(t1);
            Tenant t2 = new Tenant(); t2.setLinkId("id2"); t2.setSk("POPULARLINK#id2"); t2.setIndex(3); reordered.add(t2);
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(tenants).thenReturn(reordered);
            Mockito.doNothing().when(dao).updatePopularLinksTransactional(Mockito.anyList());
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id3"); // Move id3 from index 3 to 1
            dto.setIndex(1);
            var resp = service.reorderPopularLink(dto);
            assertEquals(3, resp.getCount());
            assertEquals("id3", resp.getPopularLinkList().get(0).getLinkId());
            assertEquals("id1", resp.getPopularLinkList().get(1).getLinkId());
            assertEquals("id2", resp.getPopularLinkList().get(2).getLinkId());
        }
    }

    @Test
    void reorderPopularLink_moveDown_success() {
        PopularLinkDao dao = Mockito.mock(PopularLinkDao.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao,s3ThumbnailClient);
        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            // Initial order: id1 (1), id2 (2), id3 (3)
            List<Tenant> tenants = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Tenant t = new Tenant();
                t.setLinkId("id" + i);
                t.setSk("POPULARLINK#id" + i);
                t.setIndex(i);
                tenants.add(t);
            }
            // After moving id1 to index 3: id2 (1), id3 (2), id1 (3)
            List<Tenant> reordered = new ArrayList<>();
            Tenant t2 = new Tenant(); t2.setLinkId("id2"); t2.setSk("POPULARLINK#id2"); t2.setIndex(1); reordered.add(t2);
            Tenant t3 = new Tenant(); t3.setLinkId("id3"); t3.setSk("POPULARLINK#id3"); t3.setIndex(2); reordered.add(t3);
            Tenant t1 = new Tenant(); t1.setLinkId("id1"); t1.setSk("POPULARLINK#id1"); t1.setIndex(3); reordered.add(t1);
            Mockito.when(dao.getPopularLinksByTenant("tenant")).thenReturn(tenants).thenReturn(reordered);
            Mockito.doNothing().when(dao).updatePopularLinksTransactional(Mockito.anyList());
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("id1"); // Move id1 from index 1 to 3
            dto.setIndex(3);
            var resp = service.reorderPopularLink(dto);
            assertEquals(3, resp.getCount());
            assertEquals("id2", resp.getPopularLinkList().get(0).getLinkId());
            assertEquals("id3", resp.getPopularLinkList().get(1).getLinkId());
            assertEquals("id1", resp.getPopularLinkList().get(2).getLinkId());
        }
    }

    @Test
    void updatePopularLink_withNewIcon_shouldUploadToS3() throws IOException {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            // Create test data
            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");
            dto.setTitle("Test Title");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");

            // Mock file
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(500L);
            when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
            when(mockFile.getOriginalFilename()).thenReturn("test-icon.png");
            when(mockFile.getContentType()).thenReturn("image/png");

            // Mock DAO responses
            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            when(dao.updatePopularLink(any(Tenant.class))).thenReturn(existingTenant);

            // Act
            PopularLinkDto result = service.updatePopularLink(dto, mockFile, false, false);

            // Assert
            verify(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            assertNotNull(result.getIconKey());
            assertEquals("test-icon.png", result.getIconFileName());
        }
    }

    @Test
    void updatePopularLink_withExistingIcon_shouldDeleteOldAndUploadNew() throws IOException {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");
            existingTenant.setIconKey("old-key");
            existingTenant.setIconFileName("old-icon.png");

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(500L);
            when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
            when(mockFile.getOriginalFilename()).thenReturn("new-icon.png");
            when(mockFile.getContentType()).thenReturn("image/png");

            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            when(dao.updatePopularLink(any(Tenant.class))).thenReturn(existingTenant);

            // Act
            PopularLinkDto result = service.updatePopularLink(dto, mockFile, false, false);

            // Assert
            verify(s3ThumbnailClient).deleteObject(any(DeleteObjectRequest.class));
            verify(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            assertNotNull(result.getIconKey());
            assertEquals("new-icon.png", result.getIconFileName());
        }
    }

    @Test
    void updatePopularLink_withRemoveExistingFile_shouldDeleteFromS3() {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");
            existingTenant.setIconKey("old-key");
            existingTenant.setIconFileName("old-icon.png");

            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            when(dao.updatePopularLink(any(Tenant.class))).thenReturn(existingTenant);

            // Act
            PopularLinkDto result = service.updatePopularLink(dto, null, false, true);

            // Assert
            verify(s3ThumbnailClient).deleteObject(any(DeleteObjectRequest.class));
            assertNull(result.getIconKey());
            assertNull(result.getIconFileName());
        }
    }

    @Test
    void updatePopularLink_withKeepExistingFile_shouldNotModifyIcon() {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");
            existingTenant.setIconKey("existing-key");
            existingTenant.setIconFileName("existing-icon.png");

            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            when(dao.updatePopularLink(any(Tenant.class))).thenReturn(existingTenant);

            // Act
            PopularLinkDto result = service.updatePopularLink(dto, null, true, false);

            // Assert
            verify(s3ThumbnailClient, never()).deleteObject(any(DeleteObjectRequest.class));
            verify(s3ThumbnailClient, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            assertEquals("existing-key", result.getIconKey());
            assertEquals("existing-icon.png", result.getIconFileName());
        }
    }



    @Test
    void updatePopularLink_withS3DeleteFailure_shouldThrowException() {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");
            existingTenant.setIconKey("existing-key");
            existingTenant.setIconFileName("existing-icon.png");

            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            doThrow(S3Exception.builder().message("Delete failed").build())
                .when(s3ThumbnailClient).deleteObject(any(DeleteObjectRequest.class));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                service.updatePopularLink(dto, null, false, true));
        }
    }

    @Test
    void updatePopularLink_withS3UploadFailure_shouldThrowException() throws IOException {
        // Arrange
        PopularLinkDao dao = mock(PopularLinkDao.class);
        S3Client s3ThumbnailClient = mock(S3Client.class);
        PopularLinkServiceImpl service = new PopularLinkServiceImpl(dao, s3ThumbnailClient);

        try (MockedStatic<TenantUtil> tenantUtilMock = Mockito.mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = Mockito.mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("user");

            PopularLinkRequestDto dto = new PopularLinkRequestDto();
            dto.setLinkId("test-id");

            Tenant existingTenant = new Tenant();
            existingTenant.setLinkId("test-id");
            existingTenant.setPk("tenant");
            existingTenant.setSk("POPULARLINK#test-id");

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(500L);
            when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
            when(mockFile.getOriginalFilename()).thenReturn("test-icon.png");
            when(mockFile.getContentType()).thenReturn("image/png");

            when(dao.getPopularLinkByPk(anyString(), anyString())).thenReturn(existingTenant);
            doThrow(S3Exception.builder().message("Upload failed").build())
                .when(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                service.updatePopularLink(dto, mockFile, false, false));
        }
    }
}
