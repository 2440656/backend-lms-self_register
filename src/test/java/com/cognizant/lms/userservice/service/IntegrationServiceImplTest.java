package com.cognizant.lms.userservice.service;


import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.IntegrationDao;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationListResponse;
import com.cognizant.lms.userservice.dto.IntegrationSummaryResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.exception.SFTPConnectionException;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.cognizant.lms.userservice.utils.CommonUtil;
import com.cognizant.lms.userservice.utils.SFTPUtil;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.validations.ContentMappingValidator;
import com.cognizant.lms.userservice.validations.CoreConfigurationValidator;
import com.cognizant.lms.userservice.validations.GeneralInformationValidator;
import com.cognizant.lms.userservice.validations.MetadataMappingValidator;
import com.cognizant.lms.userservice.validations.SettingsValidator;
import com.cognizant.lms.userservice.validations.SFTPIntegrationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

public class IntegrationServiceImplTest {


  @Mock
  private IntegrationDao integrationDao;
  @Mock
  private GeneralInformationValidator generalInformationValidator;
  @Mock
  private CoreConfigurationValidator coreConfigurationValidator;
  @Mock
  private SettingsValidator settingsValidator;
  @Mock
  private ContentMappingValidator contentMappingValidator;
  @Mock
  private MetadataMappingValidator metadataMappingValidator;

    @Mock
    private SFTPUtil sftpUtil;

    @InjectMocks
    private IntegrationServiceImpl integrationService;

  @Mock
  private SFTPIntegrationValidator sftpIntegrationValidator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

    // --- getIntegrationSummary tests ---

    @Test
    void getIntegrationSummary_validRequest_returnsData() {
        IntegrationListResponse listResponse = new IntegrationListResponse();
        IntegrationDto dto = IntegrationDto.builder().lmsIntegrationId("id1").build();
        listResponse.setIntegrationList(List.of(dto));
        listResponse.setCount(1);
        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("pk", AttributeValue.builder().s("val").build());
        listResponse.setLastEvaluatedKey(lastKey);

        when(integrationDao.getIntegrations(any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(listResponse);

        try (MockedStatic<Base64Util> base64UtilMock = mockStatic(Base64Util.class)) {
            base64UtilMock.when(() -> Base64Util.encodeLastEvaluatedKey(any())).thenReturn("encodedKey");

            IntegrationSummaryResponse response = integrationService.getIntegrationSummary(
                    "sort", "asc", null, 10, "Draft", "TypeA", "abc");

            assertEquals(HttpStatus.OK.value(), response.getStatus());
            assertEquals(1, response.getCount());
            assertNotNull(response.getData());
            assertEquals("encodedKey", response.getLastEvaluatedKey());
            assertNull(response.getError());
        }
    }

    @Test
    void getIntegrationSummary_emptyList_returnsNoContent() {
        IntegrationListResponse listResponse = new IntegrationListResponse();
        listResponse.setIntegrationList(Collections.emptyList());
        listResponse.setCount(0);
        listResponse.setLastEvaluatedKey(null);

        when(integrationDao.getIntegrations(any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(listResponse);

        IntegrationSummaryResponse response = integrationService.getIntegrationSummary(
                "sort", "asc", null, 10, "Draft", "TypeA", "abc");

        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
        assertEquals(0, response.getCount());
        assertEquals("No integrations found", response.getError());
    }

    @Test
    void getIntegrationSummary_invalidBase64Key_returnsBadRequest() {
        try (MockedStatic<Base64Util> base64UtilMock = mockStatic(Base64Util.class)) {
            base64UtilMock.when(() -> Base64Util.decodeEvaluatedKey("badkey"))
                    .thenThrow(new IllegalArgumentException("bad base64"));

            IntegrationSummaryResponse response = integrationService.getIntegrationSummary(
                    "sort", "asc", "badkey", 10, "Draft", "TypeA", "abc");

            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
            assertEquals("Invalid Base64 encoded lastEvaluatedKey", response.getError());
        }
    }

    @Test
    void getIntegrationSummary_searchValueTooShort_returnsBadRequest() {
        IntegrationSummaryResponse response = integrationService.getIntegrationSummary(
                "sort", "asc", null, 10, "Draft", "TypeA", "ab");
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("Value must contain at least 3 characters", response.getError());
    }

    // --- getIntegrationByLmsIntegrationId tests ---

    @Test
    void getIntegrationByLmsIntegrationId_foundGeneralInfo_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto integration = IntegrationDto.builder()
                    .provider("prov").integrationId("iid").integrationOwner("owner").build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.GENERAL_INFORMATION_PAGE);
            assertEquals("prov", result.getProvider());
            assertEquals("iid", result.getIntegrationId());
            assertEquals("owner", result.getIntegrationOwner());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundCoreConfig_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto.FieldEntry fieldEntry = new IntegrationDto.FieldEntry("f1", "value");
            IntegrationDto integration = IntegrationDto.builder()
                    .hostName("host").clientId("cid").organizationId("org")
                    .clientSecret("secret").fields(List.of(fieldEntry)).build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.CORE_CONFIGURATION_PAGE);
            assertEquals("host", result.getHostName());
            assertEquals("cid", result.getClientId());
            assertEquals("org", result.getOrganizationId());
            assertEquals("secret", result.getClientSecret());
            assertEquals(List.of(fieldEntry), result.getFields());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundOtherPage_returnsFullDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto integration = IntegrationDto.builder().lmsIntegrationId("id1").build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", "OtherPage");
            assertEquals("id1", result.getLmsIntegrationId());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_notFound_throwsDataBaseException() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(null);

            Exception ex = assertThrows(DataBaseException.class, () ->
                    integrationService.getIntegrationByLmsIntegrationId("id2", Constants.GENERAL_INFORMATION_PAGE));
            assertTrue(ex.getMessage().contains("Integration not found"));
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_daoThrowsException_wrapsInDataBaseException() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenThrow(new RuntimeException("DB error"));

            Exception ex = assertThrows(DataBaseException.class, () ->
                    integrationService.getIntegrationByLmsIntegrationId("id3", Constants.GENERAL_INFORMATION_PAGE));
            assertTrue(ex.getMessage().contains("Error fetching integration"));
        }
    }

    // --- saveIntegration tests ---

    @Test
    void saveIntegration_invalidPageName_throwsException() {
        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName("InvalidPage");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> integrationService.saveIntegration(request));
        assertTrue(ex.getMessage().contains("Invalid page name"));
    }

    @Test
    void saveIntegration_generalInformationPage_generatesIdAndSaves() {
        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.GENERAL_INFORMATION_PAGE);
        request.setGeneralInformation(new IntegrationDraftRequest.GeneralInformation());
        request.setIntegrationType("TypeA");
        request.setLmsIntegrationId(null);

        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueId).thenReturn("genId");
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(generalInformationValidator).validate(any());
            doNothing().when(integrationDao).saveGeneralInformation(anyString(), anyString(), anyString(), anyString(), any());

            IntegrationDto dto = integrationService.saveIntegration(request);

            assertEquals("genId", dto.getLmsIntegrationId());
            assertEquals("TypeA", dto.getIntegrationType());
            verify(generalInformationValidator).validate(any());
            verify(integrationDao).saveGeneralInformation(anyString(), anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void saveIntegration_coreConfigurationPage_missingId_throwsException() {
        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.CORE_CONFIGURATION_PAGE);
        request.setLmsIntegrationId(null);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> integrationService.saveIntegration(request));
        assertTrue(ex.getMessage().contains("lmsIntegrationId must be provided"));
    }

    @Test
    void saveIntegration_coreConfigurationPage_valid_saves() {
        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.CORE_CONFIGURATION_PAGE);
        request.setLmsIntegrationId("id123");
        request.setIntegrationType("TypeB");
        request.setCoreConfiguration(new IntegrationDraftRequest.CoreConfiguration());

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(coreConfigurationValidator).validate(any());
            doNothing().when(integrationDao).saveCoreConfiguration(anyString(), anyString(), anyString(), any());

            IntegrationDto dto = integrationService.saveIntegration(request);

            assertEquals("id123", dto.getLmsIntegrationId());
            assertEquals("TypeB", dto.getIntegrationType());
            verify(coreConfigurationValidator).validate(any());
            verify(integrationDao).saveCoreConfiguration(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void saveIntegration_daoThrowsException_wrapsInDataBaseException() {
        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.GENERAL_INFORMATION_PAGE);
        request.setGeneralInformation(new IntegrationDraftRequest.GeneralInformation());
        request.setIntegrationType("TypeA");

        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class);
             MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueId).thenReturn("genId");
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(generalInformationValidator).validate(any());
            doThrow(new RuntimeException("DB error")).when(integrationDao).saveGeneralInformation(anyString(), anyString(), anyString(), anyString(), any());

            Exception ex = assertThrows(DataBaseException.class, () -> integrationService.saveIntegration(request));
            assertTrue(ex.getMessage().contains("Error saving integration draft"));
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundGeneralInfoEdit_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto integration = IntegrationDto.builder()
                    .provider("prov")
                    .integrationId("iid")
                    .integrationOwner("owner")
                    .status("Active")
                    .reasonForChange("Update status")
                    .build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.GENERAL_INFORMATION_PAGE);
            assertEquals("prov", result.getProvider());
            assertEquals("iid", result.getIntegrationId());
            assertEquals("owner", result.getIntegrationOwner());
            assertEquals("Active", result.getStatus());
            assertEquals("Update status", result.getReasonForChange());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundCoreConfigEdit_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto.FieldEntry fieldEntry = new IntegrationDto.FieldEntry("f1", "value");
            IntegrationDto integration = IntegrationDto.builder()
                    .hostName("host")
                    .clientId("cid")
                    .organizationId("org")
                    .clientSecret("secret")
                    .testConnection("true")
                    .fields(List.of(fieldEntry))
                    .build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.CORE_CONFIGURATION_PAGE);
            assertEquals("host", result.getHostName());
            assertEquals("cid", result.getClientId());
            assertEquals("org", result.getOrganizationId());
            assertEquals("secret", result.getClientSecret());
            assertEquals("true", result.getTestConnection());
            assertEquals(List.of(fieldEntry), result.getFields());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundSettings_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto.UniqueIdentifiers uniqueIdentifier1 = new IntegrationDto.UniqueIdentifiers("id1", "Type1");
            IntegrationDto integration = IntegrationDto.builder()
                    .authenticationMethod("OAuth")
                    .syncType("FULL")
                    .syncSchedule("WEEKLY")
                    .weekDay("Monday")
                    .syncTime("10:00")
                    .identifiersList(List.of(uniqueIdentifier1))
                    .build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.SETTINGS_PAGE);
            assertEquals("OAuth", result.getAuthenticationMethod());
            assertEquals("FULL", result.getSyncType());
            assertEquals("WEEKLY", result.getSyncSchedule());
            assertEquals("Monday", result.getWeekDay());
            assertEquals("10:00", result.getSyncTime());
            assertEquals(List.of(uniqueIdentifier1), result.getIdentifiersList());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundContentMapping_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto.ContentTypeMapping contentTypeMapping1 = new IntegrationDto.ContentTypeMapping("Video", "VideoType");
            IntegrationDto.CategoryTypeMapping categoryTypeMapping1 = new IntegrationDto.CategoryTypeMapping("TypeA", "TypeB");
            IntegrationDto.CompletionSyncMapping completionSyncMapping1 = new IntegrationDto.CompletionSyncMapping("Completed", "Done");
            IntegrationDto integration = IntegrationDto.builder()
                    .contentTypeMapping(List.of(contentTypeMapping1))
                    .categoryMappingType("TypeA")
                    .categoryName("Category1")
                    .categoryTypeMapping(List.of(categoryTypeMapping1))
                    .completionSyncMapping(List.of(completionSyncMapping1))
                    .build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.CONTENT_MAPPING_PAGE);
            assertEquals(List.of(contentTypeMapping1), result.getContentTypeMapping());
            assertEquals("TypeA", result.getCategoryMappingType());
            assertEquals("Category1", result.getCategoryName());
            assertEquals(List.of(categoryTypeMapping1), result.getCategoryTypeMapping());
            assertEquals(List.of(completionSyncMapping1), result.getCompletionSyncMapping());
        }
    }

    @Test
    void getIntegrationByLmsIntegrationId_foundMetadataMapping_returnsDto() {
        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");
            IntegrationDto.MetaDataMappings metaDataMapping1 = new IntegrationDto.MetaDataMappings("meta1", "field1");
            IntegrationDto integration = IntegrationDto.builder()
                    .prefix("PRE")
                    .metaDataMappings(List.of(metaDataMapping1))
                    .build();
            when(integrationDao.getIntegrationByPartitionKey(anyString())).thenReturn(integration);

            IntegrationDto result = integrationService.getIntegrationByLmsIntegrationId("id1", Constants.METADATA_MAPPING_PAGE);
            assertEquals("PRE", result.getPrefix());
            assertEquals(List.of(metaDataMapping1), result.getMetaDataMappings());
        }
    }

    @Test
    void saveIntegration_settingsPage_valid_saves() {
        IntegrationDraftRequest.Settings settings = new IntegrationDraftRequest.Settings();
        settings.setAuthenticationMethod("OAuth");
        settings.setIdentifiersList(List.of(new IntegrationDraftRequest.UniqueIdentifiers("id1", "ss1")));
        settings.setSyncType("FULL");
        settings.setSyncSchedule("WEEKLY");
        settings.setWeekDay("Monday");
        settings.setSyncTime("10:00");

        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.SETTINGS_PAGE);
        request.setLmsIntegrationId("id123");
        request.setIntegrationType("TypeA");
        request.setSettings(settings);

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(settingsValidator).validate(any());
            doNothing().when(integrationDao).saveSettings(anyString(), anyString(), anyString(), any());

            IntegrationDto dto = integrationService.saveIntegration(request);

            assertEquals("id123", dto.getLmsIntegrationId());
            assertEquals("TypeA", dto.getIntegrationType());
            verify(settingsValidator).validate(any());
            verify(integrationDao).saveSettings(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void saveIntegration_contentMappingPage_valid_saves() {
        IntegrationDraftRequest.ContentMapping contentMapping = new IntegrationDraftRequest.ContentMapping();
        contentMapping.setContentTypeMapping(List.of(new IntegrationDraftRequest.ContentTypeMapping("Video", "SkillSpringVideo")));
        contentMapping.setCategoryMappingType("TypeA");
        contentMapping.setCategoryName("Category1");
        contentMapping.setCategoryTypeMapping(List.of(new IntegrationDraftRequest.CategoryTypeMapping("TypeA", "TypeB")));
        contentMapping.setCompletionSyncMapping(List.of(new IntegrationDraftRequest.CompletionSyncMapping("Completed", "Done")));

        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.CONTENT_MAPPING_PAGE);
        request.setLmsIntegrationId("id456");
        request.setIntegrationType("TypeB");
        request.setContentMapping(contentMapping);

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(contentMappingValidator).validate(any());
            doNothing().when(integrationDao).saveContentMapping(anyString(), anyString(), anyString(), any());

            IntegrationDto dto = integrationService.saveIntegration(request);

            assertEquals("id456", dto.getLmsIntegrationId());
            assertEquals("TypeB", dto.getIntegrationType());
            verify(contentMappingValidator).validate(any());
            verify(integrationDao).saveContentMapping(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void saveIntegration_metadataMappingPage_valid_saves() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("PRE");
        metaData.setMetaDataMappings(List.of(new IntegrationDraftRequest.MetaDataMappings("meta1", "field1")));

        IntegrationDraftRequest request = new IntegrationDraftRequest();
        request.setPageName(Constants.METADATA_MAPPING_PAGE);
        request.setLmsIntegrationId("id789");
        request.setIntegrationType("TypeC");
        request.setMetaData(metaData);

        try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
            tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenant");

            doNothing().when(metadataMappingValidator).validate(any());
            doNothing().when(integrationDao).saveMetaDataMapping(anyString(), anyString(), anyString(), any());

            IntegrationDto dto = integrationService.saveIntegration(request);

            assertEquals("id789", dto.getLmsIntegrationId());
            assertEquals("TypeC", dto.getIntegrationType());
            verify(metadataMappingValidator).validate(any());
            verify(integrationDao).saveMetaDataMapping(anyString(), anyString(), anyString(), any());
        }
    }

  @Test
  void saveSFTPIntegration_validRequest_savesSuccessfully() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    request.setPageName(Constants.CONFIGURATION);
    request.setIntegrationType("TypeA");
    request.setConfiguration(new SFTPIntegrationReqDto.Configuration());

    try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class);
         MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

      commonUtilMock.when(CommonUtil::generateUniqueId).thenReturn("generatedId");
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      doNothing().when(sftpIntegrationValidator).sftpIntegrationValidator(any());
      doNothing().when(integrationDao).saveSFTPConfiguration(anyString(), anyString(), anyString(), anyString(), any());

      SFTPIntegrationResponseDto response = integrationService.saveSFTPIntegration(request);

      assertNotNull(response);
      assertEquals("generatedId", response.getLmsIntegrationId());
      assertEquals("TypeA", response.getIntegrationType());
      verify(sftpIntegrationValidator).sftpIntegrationValidator(any());
      verify(integrationDao).saveSFTPConfiguration(anyString(), anyString(), anyString(), anyString(), any());
    }
  }

  @Test
  void saveSFTPIntegration_invalidPageName_throwsException() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    request.setPageName("InvalidPage");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        integrationService.saveSFTPIntegration(request));

    assertEquals("Invalid page name: InvalidPage", exception.getMessage());
  }

  @Test
  void saveSFTPIntegration_daoThrowsException_wrapsInDataBaseException() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    request.setPageName(Constants.CONFIGURATION);
    request.setIntegrationType("TypeA");
    request.setConfiguration(new SFTPIntegrationReqDto.Configuration());

    try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class);
         MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

      commonUtilMock.when(CommonUtil::generateUniqueId).thenReturn("generatedId");
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      doNothing().when(sftpIntegrationValidator).sftpIntegrationValidator(any());
      doThrow(new RuntimeException("DB error")).when(integrationDao).saveSFTPConfiguration(anyString(), anyString(), anyString(), anyString(), any());

      DataBaseException exception = assertThrows(DataBaseException.class, () ->
          integrationService.saveSFTPIntegration(request));

      assertTrue(exception.getMessage().contains("Error saving integration draft"));
    }
  }

  @Test
  void saveSFTPIntegration_nullLmsIntegrationId_generatesId() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    request.setPageName(Constants.CONFIGURATION);
    request.setIntegrationType("TypeA");
    request.setConfiguration(new SFTPIntegrationReqDto.Configuration());
    request.setLmsIntegrationId(null);

    try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class);
         MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {

      commonUtilMock.when(CommonUtil::generateUniqueId).thenReturn("generatedId");
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      doNothing().when(sftpIntegrationValidator).sftpIntegrationValidator(any());
      doNothing().when(integrationDao).saveSFTPConfiguration(anyString(), anyString(), anyString(), anyString(), any());

      SFTPIntegrationResponseDto response = integrationService.saveSFTPIntegration(request);

      assertNotNull(response);
      assertEquals("generatedId", response.getLmsIntegrationId());
      verify(sftpIntegrationValidator).sftpIntegrationValidator(any());
      verify(integrationDao).saveSFTPConfiguration(anyString(), anyString(), anyString(), anyString(), any());
    }
  }

  // --- Test cases for getSftpIntegrationByLmsIntegrationId ---
  @Test
  void getSftpIntegrationByLmsIntegrationId_validRequest_returnsDto() {
    try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      SFTPIntegrationResponseDto integration = SFTPIntegrationResponseDto.builder()
          .provider("provider")
          .syncType("FULL")
          .location("location")
          .userName("user")
          .password("password")
          .host("host")
          .port("22")
          .testConnection("true")
          .build();

      when(integrationDao.getSFTPIntegrationByPartitionKey(anyString(), anyBoolean())).thenReturn(integration);

      SFTPIntegrationResponseDto result = integrationService.getSftpIntegrationByLmsIntegrationId("id123", Constants.CONFIGURATION, false);

      assertNotNull(result);
      assertEquals("provider", result.getProvider());
      assertEquals("true", result.getTestConnection());
    }
  }

  @Test
  void getSftpIntegrationByLmsIntegrationId_integrationNotFound_throwsDataBaseException() {
    try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      when(integrationDao.getSFTPIntegrationByPartitionKey(anyString(), anyBoolean())).thenReturn(null);

      DataBaseException exception = assertThrows(DataBaseException.class, () ->
          integrationService.getSftpIntegrationByLmsIntegrationId("id123", Constants.CONFIGURATION, false));

      assertTrue(exception.getMessage().contains("SFTP Integration not found"));
    }
  }

  @Test
  void getSftpIntegrationByLmsIntegrationId_daoThrowsException_wrapsInDataBaseException() {
    try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      when(integrationDao.getSFTPIntegrationByPartitionKey(anyString(), anyBoolean())).thenThrow(new RuntimeException("DB error"));

      DataBaseException exception = assertThrows(DataBaseException.class, () ->
          integrationService.getSftpIntegrationByLmsIntegrationId("id123", Constants.CONFIGURATION, false));

      assertTrue(exception.getMessage().contains("Error fetching SFTP integration"));
    }
  }

  @Test
  void getSftpIntegrationByLmsIntegrationId_nullPointerException_wrapsInDataBaseException() {
    try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      when(integrationDao.getSFTPIntegrationByPartitionKey(anyString(), anyBoolean())).thenThrow(new NullPointerException("Null value"));

      DataBaseException exception = assertThrows(DataBaseException.class, () ->
          integrationService.getSftpIntegrationByLmsIntegrationId("id123", Constants.CONFIGURATION, false));

      assertTrue(exception.getMessage().contains("SFTP Integration not found"));
    }
  }

  @Test
  void getSftpIntegrationByLmsIntegrationId_invalidPageName_returnsFullDto() {
    try (MockedStatic<TenantUtil> tenantUtilMock = mockStatic(TenantUtil.class)) {
      tenantUtilMock.when(TenantUtil::getTenantCode).thenReturn("tenantCode");

      SFTPIntegrationResponseDto integration = SFTPIntegrationResponseDto.builder()
          .provider("provider")
          .syncType("FULL")
          .location("location")
          .userName("user")
          .password("password")
          .host("host")
          .port("22")
          .testConnection("true")
          .build();

      when(integrationDao.getSFTPIntegrationByPartitionKey(anyString(), anyBoolean())).thenReturn(integration);

      SFTPIntegrationResponseDto result = integrationService.getSftpIntegrationByLmsIntegrationId("id123", "InvalidPage", false);

      assertNotNull(result);
      assertEquals("provider", result.getProvider());
      assertEquals("true", result.getTestConnection());
    }
  }
    @Test
    void sftpTestConnection_usernamePasswordAuthentication_success() throws Exception {
        when(sftpUtil.checkUsernamePasswordAuthConnection(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("Connection successful");
        String result = integrationService.sftpTestConnection("user", "password",
            "location", "22", "host");
        assertEquals("Connection successful", result);
        verify(sftpUtil).checkUsernamePasswordAuthConnection("user", "password",
            "location", "22", "host");
    }


    @Test
    void sftpTestConnection_usernamePasswordAuthentication_failure() throws Exception {
        when(sftpUtil.checkUsernamePasswordAuthConnection(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new SFTPConnectionException("Username-password authentication failed"));
        SFTPConnectionException exception = assertThrows(SFTPConnectionException.class, () ->
            integrationService.sftpTestConnection("user", "password", "location",
                "22", "host"));
        assertEquals("Username-password authentication failed", exception.getMessage());
        verify(sftpUtil).checkUsernamePasswordAuthConnection("user",
            "password", "location", "22", "host");
    }

  // Test case for unexpected exception during SFTP connection test
  @Test
  void sftpTestConnection_unexpectedException_throwsSFTPConnectionException() throws Exception {
    when(sftpUtil.checkUsernamePasswordAuthConnection(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new RuntimeException("Unexpected error"));

    SFTPConnectionException exception = assertThrows(SFTPConnectionException.class, () ->
        integrationService.sftpTestConnection("user", "password", "location", "22", "host"));

    assertTrue(exception.getMessage().contains("Unexpected error during SFTP connection test"));
    verify(sftpUtil).checkUsernamePasswordAuthConnection("user", "password", "location", "22", "host");
  }

}
