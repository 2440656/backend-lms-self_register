package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UserSettingsRequestDto;
import com.cognizant.lms.userservice.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSettingsControllerTest {

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private UserSettingsController userSettingsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserSettingsByEmailId_found() {
        UserSettings userSettings = new UserSettings();
        userSettings.setEmailId("test@example.com");
        when(userSettingsService.getUserSettingsByEmailId("test@example.com")).thenReturn(userSettings);

        ResponseEntity<HttpResponse> response = userSettingsController.getUserSettingsByEmailId("test@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userSettings, response.getBody().getData());
    }

    @Test
    void getUserSettingsByEmailId_notFound() {
        when(userSettingsService.getUserSettingsByEmailId("notfound@example.com")).thenReturn(null);

        ResponseEntity<HttpResponse> response = userSettingsController.getUserSettingsByEmailId("notfound@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().getData());
        assertTrue(response.getBody().getError().contains("notfound@example.com"));
    }

    @Test
    void createUserSettings_success() {
        UserSettingsRequestDto requestDto = new UserSettingsRequestDto();
        UserSettings userSettings = new UserSettings();
        userSettings.setEmailId("test@example.com");
        when(userSettingsService.createUserSettings(requestDto)).thenReturn(userSettings);

        ResponseEntity<String> response = userSettingsController.createUserSettings(requestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createUserSettings_failure() {
        UserSettingsRequestDto requestDto = new UserSettingsRequestDto();
        when(userSettingsService.createUserSettings(requestDto)).thenReturn(null);

        ResponseEntity<String> response = userSettingsController.createUserSettings(requestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed"));
    }

    @Test
    void migrateUserTypeField_success() {
        HttpResponse httpResponse = new HttpResponse();
        when(userSettingsService.migrateUserTypeField("key", 100)).thenReturn(httpResponse);

        ResponseEntity<HttpResponse> response = userSettingsController.migrateUserTypeField("key", 100);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(httpResponse, response.getBody());
    }
}