package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LmsUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private LmsUserController lmsUserController;

    @BeforeEach
    public void setUp() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
    }

    @Test
    void getUserDetailsByEmailId_ReturnsUserDetails_WhenUserExists() {
        String email = "test@example.com";
        String status = "active";
        User mockUser = new User();
        mockUser.setPk("user1");
        HttpResponse expectedResponse = new HttpResponse();
        expectedResponse.setData(mockUser);
        expectedResponse.setStatus(HttpStatus.OK.value());

        when(userService.getUserByEmailId(email, status)).thenReturn(mockUser);

        ResponseEntity<HttpResponse> response = lmsUserController.getUserDetailsByEmailId(email, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse.getStatus(), response.getBody().getStatus());
        assertEquals(mockUser, response.getBody().getData());
    }

    @Test
    void getUserDetailsByEmailId_ReturnsNoContent_WhenUserDoesNotExist() {
        String email = "nonexistent@example.com";
        String status = "inactive";
        HttpResponse expectedResponse = new HttpResponse();
        expectedResponse.setStatus(HttpStatus.NO_CONTENT.value());
        expectedResponse.setError("User not found with emailId: " + email);
        when(userService.getUserByEmailId(email, status)).thenReturn(null);
        ResponseEntity<HttpResponse> response = lmsUserController.getUserDetailsByEmailId(email, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse.getStatus(), response.getBody().getStatus());
        assertEquals(expectedResponse.getError(), response.getBody().getError());
    }

    @Test
    void getUserDetailsByEmailId_ReturnsUserDetails_WhenStatusIsNull() {
        String email = "test@example.com";
        String status = null;
        User mockUser = new User();
        mockUser.setPk("user2");
        HttpResponse expectedResponse = new HttpResponse();
        expectedResponse.setData(mockUser);
        expectedResponse.setStatus(HttpStatus.OK.value());
        when(userService.getUserByEmailId(email, status)).thenReturn(mockUser);
        ResponseEntity<HttpResponse> response = lmsUserController.getUserDetailsByEmailId(email, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse.getStatus(), response.getBody().getStatus());
        assertEquals(mockUser, response.getBody().getData());
    }

    @Test
    void getUserCountByTenantCode_ReturnsCounts_ForMultipleTenantCodes() {
        List<String> tenantCodes = List.of("t-1", "t-2");
        when(userService.getUserCountByTenantCodes(tenantCodes)).thenReturn(Map.of("t-1", 2, "t-2", 5));

        ResponseEntity<Map<String, Object>> response = lmsUserController.getUserCountByTenantCode(tenantCodes);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(tenantCodes, response.getBody().get("tenantCodes"));
        assertEquals(Map.of("t-1", 2, "t-2", 5), response.getBody().get("counts"));

        verify(userService, times(1)).getUserCountByTenantCodes(tenantCodes);
    }

    @Test
    void getUserCountByTenantCode_SplitsCommaSeparatedTenantCodes_AndReturnsCounts() {
        List<String> requestTenantCodes = List.of("t-1,t-2", "t-3");
        List<String> normalized = List.of("t-1", "t-2", "t-3");

        when(userService.getUserCountByTenantCodes(normalized)).thenReturn(Map.of("t-1", 1, "t-2", 0, "t-3", 4));

        ResponseEntity<Map<String, Object>> response = lmsUserController.getUserCountByTenantCode(requestTenantCodes);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(normalized, response.getBody().get("tenantCodes"));
        assertEquals(Map.of("t-1", 1, "t-2", 0, "t-3", 4), response.getBody().get("counts"));

        verify(userService, times(1)).getUserCountByTenantCodes(normalized);
    }
}
