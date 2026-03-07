package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cognizant.lms.userservice.dao.QuickLinkDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.QuickLinkDto;
import com.cognizant.lms.userservice.dto.QuickLinkRequestDto;
import com.cognizant.lms.userservice.dto.QuickLinksResponse;
import com.cognizant.lms.userservice.exception.QuickLinkLimitException;
import com.cognizant.lms.userservice.exception.QuickLinkNotFoundException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import com.cognizant.lms.userservice.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class QuickLinkServiceImplTest {

    @Mock
    private QuickLinkDao quickLinkDao;

    @InjectMocks
    private QuickLinkServiceImpl quickLinkService;

    private static final String TENANT_CODE = "TENANT_001";
    private static final String QUICKLINK_PREFIX = "QUICKLINK#";
    private static final String TEST_LINK_ID = "link-123";
    private static final String TEST_USERNAME = "testuser";

    // ==================== getAllQuickLinks Tests ====================

    @Test
    @DisplayName("Test getAllQuickLinks - With Data")
    void testGetAllQuickLinks_WithData() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            List<Tenant> links = new ArrayList<>();
            Tenant link1 = new Tenant();
            link1.setLinkId("link-1");
            link1.setTitle("Link 1");
            link1.setUrl("https://link1.com");
            link1.setDescription("Description 1");
            link1.setIndex(1);

            links.add(link1);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            QuickLinksResponse result = quickLinkService.getAllQuickLinks();

            assertNotNull(result);
            assertEquals(1, result.getCount());
            assertEquals(1, result.getQuickLinks().size());
            assertEquals("Link 1", result.getQuickLinks().get(0).getTitle());
            assertEquals("link-1", result.getQuickLinks().get(0).getLinkId());
        }
    }

    @Test
    @DisplayName("Test getAllQuickLinks - Sorted by Index")
    void testGetAllQuickLinks_SortedByIndex() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            List<Tenant> links = new ArrayList<>();
            Tenant link3 = new Tenant();
            link3.setLinkId("link-3");
            link3.setTitle("Link 3");
            link3.setIndex(3);

            Tenant link1 = new Tenant();
            link1.setLinkId("link-1");
            link1.setTitle("Link 1");
            link1.setIndex(1);

            Tenant link2 = new Tenant();
            link2.setLinkId("link-2");
            link2.setTitle("Link 2");
            link2.setIndex(2);

            links.add(link3);
            links.add(link1);
            links.add(link2);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            QuickLinksResponse result = quickLinkService.getAllQuickLinks();

            assertNotNull(result);
            assertEquals(3, result.getCount());
            List<QuickLinkDto> sortedLinks = result.getQuickLinks();
            assertEquals(1, sortedLinks.get(0).getIndex());
            assertEquals(2, sortedLinks.get(1).getIndex());
            assertEquals(3, sortedLinks.get(2).getIndex());
        }
    }

    // ==================== reorderQuickLink Tests ====================

    @Test
    @DisplayName("Test reorderQuickLink - Success Move Down")
    void testReorderQuickLink_SuccessMoveDown() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId(TEST_LINK_ID);
            request.setIndex(3);

            List<Tenant> links = new ArrayList<>();

            Tenant linkToMove = new Tenant();
            linkToMove.setLinkId(TEST_LINK_ID);
            linkToMove.setSk(QUICKLINK_PREFIX + TEST_LINK_ID);
            linkToMove.setIndex(1);

            Tenant link2 = new Tenant();
            link2.setLinkId("link-2");
            link2.setSk(QUICKLINK_PREFIX + "link-2");
            link2.setIndex(2);

            Tenant link3 = new Tenant();
            link3.setLinkId("link-3");
            link3.setSk(QUICKLINK_PREFIX + "link-3");
            link3.setIndex(3);

            links.add(linkToMove);
            links.add(link2);
            links.add(link3);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            QuickLinksResponse result = quickLinkService.reorderQuickLink(request);

            assertNotNull(result);
            verify(quickLinkDao, times(1)).updateQuickLinksTransactional(any());
        }
    }

    @Test
    @DisplayName("Test reorderQuickLink - Success Move Up")
    void testReorderQuickLink_SuccessMoveUp() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId(TEST_LINK_ID);
            request.setIndex(1);

            List<Tenant> links = new ArrayList<>();

            Tenant linkToMove = new Tenant();
            linkToMove.setLinkId(TEST_LINK_ID);
            linkToMove.setSk(QUICKLINK_PREFIX + TEST_LINK_ID);
            linkToMove.setIndex(3);

            Tenant link1 = new Tenant();
            link1.setLinkId("link-1");
            link1.setSk(QUICKLINK_PREFIX + "link-1");
            link1.setIndex(1);

            Tenant link2 = new Tenant();
            link2.setLinkId("link-2");
            link2.setSk(QUICKLINK_PREFIX + "link-2");
            link2.setIndex(2);

            links.add(link1);
            links.add(link2);
            links.add(linkToMove);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            QuickLinksResponse result = quickLinkService.reorderQuickLink(request);

            assertNotNull(result);
            verify(quickLinkDao, times(1)).updateQuickLinksTransactional(any());
        }
    }

    @Test
    @DisplayName("Test reorderQuickLink - No Change Needed")
    void testReorderQuickLink_NoChangeNeeded() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId(TEST_LINK_ID);
            request.setIndex(1);

            List<Tenant> links = new ArrayList<>();
            Tenant linkToMove = new Tenant();
            linkToMove.setLinkId(TEST_LINK_ID);
            linkToMove.setSk(QUICKLINK_PREFIX + TEST_LINK_ID);
            linkToMove.setIndex(1);

            links.add(linkToMove);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            QuickLinksResponse result = quickLinkService.reorderQuickLink(request);

            assertNotNull(result);
            verify(quickLinkDao, never()).updateQuickLinksTransactional(any());
        }
    }

    @Test
    @DisplayName("Test reorderQuickLink - Null LinkId")
    void testReorderQuickLink_NullLinkId() {
        QuickLinkRequestDto request = new QuickLinkRequestDto();
        request.setLinkId(null);
        request.setIndex(2);

        assertThrows(IllegalArgumentException.class, () -> quickLinkService.reorderQuickLink(request));
    }

    @Test
    @DisplayName("Test reorderQuickLink - Null Index")
    void testReorderQuickLink_NullIndex() {
        QuickLinkRequestDto request = new QuickLinkRequestDto();
        request.setLinkId(TEST_LINK_ID);
        request.setIndex(null);

        assertThrows(IllegalArgumentException.class, () -> quickLinkService.reorderQuickLink(request));
    }

    @Test
    @DisplayName("Test reorderQuickLink - Not Found")
    void testReorderQuickLink_NotFound() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId("invalid-id");
            request.setIndex(1);

            List<Tenant> links = new ArrayList<>();

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            assertThrows(QuickLinkNotFoundException.class, () -> quickLinkService.reorderQuickLink(request));
        }
    }

    @Test
    @DisplayName("Test reorderQuickLink - Index Out of Bounds")
    void testReorderQuickLink_IndexOutOfBounds() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId(TEST_LINK_ID);
            request.setIndex(5);

            List<Tenant> links = new ArrayList<>();
            Tenant linkToMove = new Tenant();
            linkToMove.setLinkId(TEST_LINK_ID);
            linkToMove.setSk(QUICKLINK_PREFIX + TEST_LINK_ID);
            linkToMove.setIndex(1);

            links.add(linkToMove);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            assertThrows(IllegalArgumentException.class, () -> quickLinkService.reorderQuickLink(request));
        }
    }

    @Test
    @DisplayName("Test reorderQuickLink - Negative Index")
    void testReorderQuickLink_NegativeIndex() {
        try (MockedStatic<TenantUtil> tenantUtil = mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn(TENANT_CODE);

            QuickLinkRequestDto request = new QuickLinkRequestDto();
            request.setLinkId(TEST_LINK_ID);
            request.setIndex(-1);

            List<Tenant> links = new ArrayList<>();
            Tenant linkToMove = new Tenant();
            linkToMove.setLinkId(TEST_LINK_ID);
            linkToMove.setSk(QUICKLINK_PREFIX + TEST_LINK_ID);
            linkToMove.setIndex(1);

            links.add(linkToMove);

            when(quickLinkDao.getQuickLinksByTenant(TENANT_CODE)).thenReturn(links);

            assertThrows(IllegalArgumentException.class, () -> quickLinkService.reorderQuickLink(request));
        }
    }
}