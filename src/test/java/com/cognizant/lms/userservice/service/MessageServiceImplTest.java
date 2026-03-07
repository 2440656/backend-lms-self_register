package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.MessageDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.MessageRequest;
import com.cognizant.lms.userservice.dto.MessageResponse;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);
    }

    private Tenant createTenant(String category, Boolean status, String message) {
        Tenant tenant = new Tenant();
        tenant.setPk("t-2");
        tenant.setSk("t-2#" + category);
        tenant.setCategory(category);
        tenant.setMessage(message);
        tenant.setMessageStatus(status);
        return tenant;
    }

    @Test
    void testPublishMessage_NewTenant() {
        MessageRequest request = new MessageRequest();
        request.setCategory("Alert");
        request.setMessage("Maintenance");
        request.setMessageStatus(true);

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-2");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("admin");

            when(messageDao.findTenantByKey("t-2", "Alert")).thenReturn(null);
            messageService.publishMessage(request);
            verify(messageDao).saveTenant(argThat(tenant ->
                    tenant.getPk().equals("t-2") &&
                            tenant.getCategory().equals("Alert") &&
                            tenant.getMessage().equals("Maintenance") &&
                            tenant.getMessageStatus().equals(true) &&
                            tenant.getCreatedBy().equals("admin")
            ));
        }
    }

    @Test
    void testPublishMessage_ExistingTenant() {
        MessageRequest request = new MessageRequest();
        request.setCategory("Alert");
        request.setMessage("Maintenance Updated");
        request.setMessageStatus(true);

        Tenant existingTenant = new Tenant();
        existingTenant.setCreatedOn("2023-01-01");
        existingTenant.setCreatedBy("admin");

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class);
             MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-2");
            userContextMock.when(UserContext::getCreatedBy).thenReturn("admin");

            when(messageDao.findTenantByKey("t-2", "Alert")).thenReturn(existingTenant);
            messageService.publishMessage(request);
            verify(messageDao).saveTenant(argThat(tenant ->
                    tenant.getUpdatedBy().equals("admin") &&
                            tenant.getCreatedBy().equals("admin") &&
                            tenant.getMessage().equals("Maintenance Updated")
            ));
        }
    }

    @Test
    void testGetMessageByCategoryAndStatus_TenantsFound() {
        Tenant tenant1 = createTenant("Alert", true, "System maintenance");
        Tenant tenant2 = createTenant("Alert", true, "Security update");

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-2");
            when(messageDao.findTenantByCategoryAndStatus("t-2", "Alert", true))
                    .thenReturn(List.of(tenant1, tenant2));

            List<MessageResponse> responses = messageService.getMessageByCategoryAndStatus("Alert", true);

            assertNotNull(responses);
            assertEquals(2, responses.size());
            assertEquals("System maintenance", responses.get(0).getMessage());
            assertEquals("Security update", responses.get(1).getMessage());
        }
    }

    @Test
    void testGetMessageByCategoryAndStatus_NoTenantsFound() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-2");
            when(messageDao.findTenantByCategoryAndStatus("t-2", "Alert", true)).thenReturn(Collections.emptyList());

            List<MessageResponse> responses = messageService.getMessageByCategoryAndStatus("Alert", true);

            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }
    }

    @Test
    void testGetMessageByCategoryAndStatus_NullCategoryAndStatus() {
        Tenant tenant = createTenant("Alert", true, "Maintenance");

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("t-2");
            when(messageDao.findTenantByCategoryAndStatus("t-2", null, null)).thenReturn(List.of(tenant));

            List<MessageResponse> responses = messageService.getMessageByCategoryAndStatus(null, null);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals("Alert", responses.get(0).getCategory());
            assertEquals("Maintenance", responses.get(0).getMessage());
            assertEquals(true, responses.get(0).getStatus());
        }
    }
}
