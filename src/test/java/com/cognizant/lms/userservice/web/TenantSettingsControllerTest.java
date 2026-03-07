package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantSettingsRequest;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.dto.UpdateTenantFeatureFlagsRequest;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.service.TenantSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantSettingsControllerTest {

    @Mock
    private TenantSettingsService tenantSettingsService;

    @InjectMocks
    private TenantSettingsController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTenantSetting_found_returnsOkWithData() {
        TenantSettingsResponse mockResponse = new TenantSettingsResponse();
        when(tenantSettingsService.getTenantSettingResponse("content-moderation")).thenReturn(mockResponse);

        ResponseEntity<HttpResponse> response = controller.getTenantSetting("content-moderation");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getStatus());
        assertEquals(mockResponse, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void getTenantSetting_notFound_returnsOkWithError() {
        when(tenantSettingsService.getTenantSettingResponse("unknown")).thenReturn(null);

        ResponseEntity<HttpResponse> response = controller.getTenantSetting("unknown");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(204, response.getBody().getStatus());
        assertNull(response.getBody().getData());
        assertTrue(response.getBody().getError().contains("Tenant setting not found"));
    }

    @Test
    void createTenantSettings_success_returnsCreated() {
        TenantSettingsRequest request = new TenantSettingsRequest();

        ResponseEntity<HttpResponse> response = controller.createTenantSettings(request);

        verify(tenantSettingsService).createTenantSettings(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Tenant Settings Created Successfully.", response.getBody().getData());
        assertEquals(HttpStatus.CREATED.value(), response.getBody().getStatus());
    }

    @Test
    void updateTenantSettings_success_returnsOk() {
        String reviewEmail = "test@example.com";
        String commentType = "typeA";

        ResponseEntity<HttpResponse> response = controller.updateTenantSettings(reviewEmail, commentType);

        verify(tenantSettingsService).updateTenantSettings(reviewEmail, commentType);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Tenant Updated Successfully.", response.getBody().getData());
        assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
    }

    // --- New tests for Feature Flags endpoints ---

    @Test
    void getTenantFeatureFlags_found_returnsOkWithData() {
        TenantFeatureFlagsDto dto = TenantFeatureFlagsDto.builder()
                .pk("PK")
                .sk("SK")
                .tenant("tenant1")
                .name("feature-flags")
                .featureFlags(Map.of("aiAssistant", true, "learningPaths", false))
                .build();
        when(tenantSettingsService.getTenantFeatureFlags()).thenReturn(dto);

        ResponseEntity<HttpResponse> response = controller.getTenantFeatureFlags();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals(dto, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void getTenantFeatureFlags_notFound_returnsOkWithError() {
        when(tenantSettingsService.getTenantFeatureFlags()).thenReturn(null);

        ResponseEntity<HttpResponse> response = controller.getTenantFeatureFlags();

        assertEquals(HttpStatus.OK, response.getStatusCode()); // controller returns 200 even when not found
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertNull(response.getBody().getData());
        assertTrue(response.getBody().getError().contains("Tenant Feature flags not found"));
    }

    @Test
    void getTenantFeatureFlags_exception_returnsInternalServerError() {
        when(tenantSettingsService.getTenantFeatureFlags()).thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<HttpResponse> response = controller.getTenantFeatureFlags();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody()); // controller returns null body on exception
    }

    @Test
    void updateTenantFeatureFlags_success_returnsOkWithData() {
        UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(Map.of("aiAssistant", true));
        TenantFeatureFlagsDto updated = TenantFeatureFlagsDto.builder()
                .pk("PK")
                .sk("SK")
                .tenant("tenant1")
                .name("feature-flags")
                .featureFlags(request.getFeatureFlags())
                .build();
        when(tenantSettingsService.updateTenantFeatureFlags(request)).thenReturn(updated);

        ResponseEntity<HttpResponse> response = controller.updateTenantFeatureFlags(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals(updated, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void updateTenantFeatureFlags_notFound_returnsNotFoundWithError() {
        UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(Map.of("aiAssistant", false));
        when(tenantSettingsService.updateTenantFeatureFlags(request)).thenReturn(null);

        ResponseEntity<HttpResponse> response = controller.updateTenantFeatureFlags(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertNull(response.getBody().getData());
        assertTrue(response.getBody().getError().contains("Tenant Feature flags record not found for update"));
    }

    @Test
    void updateTenantFeatureFlags_exception_returnsInternalServerError() {
        UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(Map.of("aiAssistant", true));
        when(tenantSettingsService.updateTenantFeatureFlags(request)).thenThrow(new RuntimeException("Boom"));

        ResponseEntity<HttpResponse> response = controller.updateTenantFeatureFlags(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}
