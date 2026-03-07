package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserSettingsDao;
import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UserSettingsRequestDto;
import com.cognizant.lms.userservice.exception.UserSettingAlreadyFoundException;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSettingsServiceImplTest {

    @Mock
    private UserSettingsDao userSettingsDao;

    private UserSettingsServiceImpl userSettingsService;

    private MockedStatic<TenantUtil> tenantUtilMockedStatic;
    private MockedStatic<UserContext> userContextMockedStatic;
    private MockedStatic<Base64Util> base64UtilMockedStatic;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userSettingsService = new UserSettingsServiceImpl(userSettingsDao);
        tenantUtilMockedStatic = mockStatic(TenantUtil.class);
        userContextMockedStatic = mockStatic(UserContext.class);
        base64UtilMockedStatic = mockStatic(Base64Util.class);
    }

    @AfterEach
    void tearDown() {
        tenantUtilMockedStatic.close();
        userContextMockedStatic.close();
        base64UtilMockedStatic.close();
    }

    @Test
    void getUserSettingsByEmailId_returnsUserSettings() {
        String email = "Test@Example.com";
        String tenantCode = "tenant1";
        String sk = tenantCode + "#" + Constants.TYPE_USER_SETTINGS;
        UserSettings userSettings = new UserSettings();

        tenantUtilMockedStatic.when(TenantUtil::getTenantCode).thenReturn(tenantCode);
        when(userSettingsDao.getUserSettingsByEmailId(email.toLowerCase(), sk)).thenReturn(userSettings);

        UserSettings result = userSettingsService.getUserSettingsByEmailId(email);

        assertEquals(userSettings, result);
    }

    @Test
    void createUserSettings_success() {
        UserSettingsRequestDto requestDto = new UserSettingsRequestDto();
        requestDto.setUserId("user1");
        requestDto.setVoiceId("voice");
        requestDto.setTheme("theme");

        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("tenantPk");
        tenantUtilMockedStatic.when(TenantUtil::getTenantDetails).thenReturn(tenantDTO);
        userContextMockedStatic.when(UserContext::getUserEmail).thenReturn("user@example.com");
        userContextMockedStatic.when(UserContext::getCreatedBy).thenReturn("creator");

        when(userSettingsDao.getUserSettingsByEmailId("user@example.com", "tenantPk#user-settings")).thenReturn(null);

        // Use builder pattern for UserSettings if needed
        doNothing().when(userSettingsDao).saveUserSettings(any(UserSettings.class));

        UserSettings result = userSettingsService.createUserSettings(requestDto);

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmailId());
        assertEquals("tenantPk", result.getTenantCode());
        assertEquals("user1", result.getPk());
    }

    @Test
    void createUserSettings_nullUserId_throwsException() {
        UserSettingsRequestDto requestDto = new UserSettingsRequestDto();
        requestDto.setUserId(null);

        assertThrows(IllegalArgumentException.class, () -> userSettingsService.createUserSettings(requestDto));
    }

    @Test
    void migrateUserTypeField_success() {
        String lastEvaluatedKeyEncoded = "encodedKey";
        Map<String, String> decodedKey = new HashMap<>();
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("key", AttributeValue.builder().s("val").build());

        base64UtilMockedStatic.when(() -> Base64Util.decodeEvaluatedKey(lastEvaluatedKeyEncoded)).thenReturn(decodedKey);
        when(userSettingsDao.updateEmptyTypeFields(decodedKey, 10)).thenReturn(attributeValueMap);
        base64UtilMockedStatic.when(() -> Base64Util.encodeLastEvaluatedKey(attributeValueMap)).thenReturn("newEncodedKey");

        HttpResponse response = userSettingsService.migrateUserTypeField(lastEvaluatedKeyEncoded, 10);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getData().toString().contains("newEncodedKey"));
    }

    @Test
    void migrateUserTypeField_invalidBase64_returnsBadRequest() {
        String lastEvaluatedKeyEncoded = "badKey";
        base64UtilMockedStatic.when(() -> Base64Util.decodeEvaluatedKey(lastEvaluatedKeyEncoded))
                .thenThrow(new IllegalArgumentException("bad base64"));

        HttpResponse response = userSettingsService.migrateUserTypeField(lastEvaluatedKeyEncoded, 10);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("Invalid Base64 encoded lastEvaluatedKey", response.getError());
    }

    @Test
    void migrateUserTypeField_noLastEvaluatedKey_returnsNullKey() {
        when(userSettingsDao.updateEmptyTypeFields(null, 5)).thenReturn(null);

        HttpResponse response = userSettingsService.migrateUserTypeField(null, 5);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getData().toString().contains("lastEvaluatedKey: null"));
    }
}