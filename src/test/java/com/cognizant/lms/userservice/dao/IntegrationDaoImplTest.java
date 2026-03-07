package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationListResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.utils.CommonUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class IntegrationDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;
    @Mock
    private DynamoDbClient dynamoDbClient;

    @InjectMocks
    private IntegrationDaoImpl integrationDao;

    @Spy
    @InjectMocks
    private IntegrationDaoImpl spyDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        integrationDao = new IntegrationDaoImpl(dynamoDBConfig, "testTable");
        spyDao = Mockito.spy(integrationDao);
    }

    @Test
    void getIntegrationByPartitionKey_returnsIntegrationDto() {
        String pk = "pk1";
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("type", AttributeValue.fromS(Constants.INTEGRATION_FIELD_TYPE));
        item1.put("fieldName", AttributeValue.fromS("field1"));
        item1.put("fieldValue", AttributeValue.fromS("value1"));

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("type", AttributeValue.fromS("integration"));
        item2.put("provider", AttributeValue.fromS("provider1"));

        QueryResponse response = QueryResponse.builder()
                .count(2)
                .items(item1, item2)
                .build();

        try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
            IntegrationDto fieldDto = new IntegrationDto();
            fieldDto.setFieldName("field1");
            fieldDto.setFieldValue("value1");
            IntegrationDto integrationDto = new IntegrationDto();
            integrationDto.setProvider("provider1");

            util.when(() -> CommonUtil.mapItemToDto(item1, IntegrationDto.class)).thenReturn(fieldDto);
            util.when(() -> CommonUtil.mapItemToDto(item2, IntegrationDto.class)).thenReturn(integrationDto);

            when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

            IntegrationDto result = integrationDao.getIntegrationByPartitionKey(pk);
            assertNotNull(result);
            assertEquals("provider1", result.getProvider());
            assertEquals(1, result.getFields().size());
            assertEquals("field1", result.getFields().get(0).getFieldName());
            assertEquals("value1", result.getFields().get(0).getFieldValue());
        }
    }

    @Test
    void getIntegrationByPartitionKey_returnsNullIfNotFound() {
        QueryResponse response = QueryResponse.builder().count(0).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);
        assertNull(integrationDao.getIntegrationByPartitionKey("pk"));
    }

    @Test
    void getIntegrationByPartitionKey_throwsDataBaseExceptionOnError() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("error").build());
        assertThrows(DataBaseException.class, () -> integrationDao.getIntegrationByPartitionKey("pk"));
    }

    @Test
    void getIntegrations_returnsListResponse() {
        QueryResponse countResponse = QueryResponse.builder().count(1).build();
        QueryResponse queryResponse = QueryResponse.builder()
                .count(1)
                .items(new HashMap<>())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(countResponse)
                .thenReturn(queryResponse);

        try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
            util.when(() -> CommonUtil.mapItemToDto(any(), eq(IntegrationDto.class)))
                    .thenReturn(new IntegrationDto());

            IntegrationListResponse result = integrationDao.getIntegrations("provider", "asc", null, 1, null, null, null);
            assertNotNull(result);
            assertEquals(1, result.getCount());
            // Use the correct getter for the list
            assertEquals(1, result.getIntegrationList().size());
        }
    }

    @Test
    void getIntegrations_throwsDataBaseExceptionOnError() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("error").build());
        assertThrows(DataBaseException.class, () -> integrationDao.getIntegrations("provider", "asc", null, 1, null, null, null));
    }


    @Test
    void saveGeneralInformation_shouldNotThrow() {
        IntegrationDraftRequest.GeneralInformation generalInfo = mock(IntegrationDraftRequest.GeneralInformation.class);
        when(generalInfo.getProvider()).thenReturn("provider");
        when(generalInfo.getIntegrationId()).thenReturn("id");
        when(generalInfo.getIntegrationOwner()).thenReturn("owner");

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getGeneralInformation()).thenReturn(generalInfo);
        when(request.getIntegrationType()).thenReturn("type");
        when(request.getLmsIntegrationId()).thenReturn("lmsId");

        // Mock AuthUser as principal
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(new HashMap<>()).build());

        assertDoesNotThrow(() -> integrationDao.saveGeneralInformation("pk", "sk", "type", "status", request));
    }

    @Test
    void saveCoreConfiguration_shouldUpdateItemAndSaveFields() {
        // Mock AuthUser in SecurityContext
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Prepare core configuration
        IntegrationDraftRequest.CoreConfiguration coreConfig = new IntegrationDraftRequest.CoreConfiguration();
        coreConfig.setHostName("host");
        coreConfig.setClientId("client");
        coreConfig.setOrganizationId("org");
        coreConfig.setClientSecret("secret");
        coreConfig.setFields(Arrays.asList(
                new IntegrationDraftRequest.FieldEntry("field1", "value1")
        ));

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getCoreConfiguration()).thenReturn(coreConfig);

        // Spy saveFields to verify call
        doNothing().when(spyDao).saveCoreConfigurationAdditionalFields(any(), any());

        // Mock updateItem call
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        assertDoesNotThrow(() -> spyDao.saveCoreConfiguration("pk", "sk", "status", request));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
        verify(spyDao, times(1)).saveCoreConfigurationAdditionalFields(eq("pk"), eq(coreConfig.getFields()));
    }

    @Test
    void saveCoreConfiguration_shouldHandleNullFields() {
        // Mock AuthUser in SecurityContext
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Prepare core configuration with null fields
        IntegrationDraftRequest.CoreConfiguration coreConfig = new IntegrationDraftRequest.CoreConfiguration();
        coreConfig.setHostName("host");
        coreConfig.setClientId("client");
        coreConfig.setOrganizationId("org");
        coreConfig.setClientSecret("secret");
        coreConfig.setFields(null);

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getCoreConfiguration()).thenReturn(coreConfig);

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        assertDoesNotThrow(() -> spyDao.saveCoreConfiguration("pk", "sk", "status", request));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
        verify(spyDao, times(1)).saveCoreConfigurationAdditionalFields(eq("pk"), eq(null));
    }

    @Test
    void saveFields_skipsNullFieldsAndCallsPutItem() {
        List<IntegrationDraftRequest.FieldEntry> fields = Arrays.asList(
                new IntegrationDraftRequest.FieldEntry("field1", "value1"),
                new IntegrationDraftRequest.FieldEntry(null, "value2"),
                new IntegrationDraftRequest.FieldEntry("field3", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveCoreConfigurationAdditionalFields("pk", fields));
    }

    @Test
    void saveSettings_shouldUpdateItemAndSaveIdentifiers() {
        IntegrationDraftRequest.Settings settings = mock(IntegrationDraftRequest.Settings.class);
        when(settings.getAuthenticationMethod()).thenReturn("authMethod");
        when(settings.getSyncType()).thenReturn("syncType");
        when(settings.getSyncSchedule()).thenReturn("schedule");
        when(settings.getWeekDay()).thenReturn("Monday");
        when(settings.getSyncTime()).thenReturn("10:00");
        IntegrationDraftRequest.UniqueIdentifiers identifier = new IntegrationDraftRequest.UniqueIdentifiers("tpId", "ssId");
        when(settings.getIdentifiersList()).thenReturn(Arrays.asList(identifier));

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getSettings()).thenReturn(settings);
        when(request.getPageName()).thenReturn("pageName");

        doNothing().when(spyDao).saveSettingsConfigurationAdditionalFields(any(), any());
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.saveSettings("pk", "sk", "status", request));
        verify(spyDao, times(1)).updateItem(any(), any(), any(), any(), any());
        verify(spyDao, times(1)).saveSettingsConfigurationAdditionalFields(eq("pk"), eq(Arrays.asList(identifier)));
    }

    @Test
    void saveSettings_shouldHandleNullIdentifiersList() {
        IntegrationDraftRequest.Settings settings = mock(IntegrationDraftRequest.Settings.class);
        when(settings.getAuthenticationMethod()).thenReturn("authMethod");
        when(settings.getSyncType()).thenReturn("syncType");
        when(settings.getSyncSchedule()).thenReturn("schedule");
        when(settings.getWeekDay()).thenReturn("Monday");
        when(settings.getSyncTime()).thenReturn("10:00");
        when(settings.getIdentifiersList()).thenReturn(null);

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getSettings()).thenReturn(settings);
        when(request.getPageName()).thenReturn("pageName");

        // Set up security context to avoid NPE
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        doNothing().when(spyDao).saveSettingsConfigurationAdditionalFields(any(), any());
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.saveSettings("pk", "sk", "status", request));
        verify(spyDao, times(1)).updateItem(any(), any(), any(), any(), any());
        verify(spyDao, times(1)).saveSettingsConfigurationAdditionalFields(eq("pk"), eq(null));
    }

    @Test
    void saveSettings_shouldHandleEmptyIdentifiersList() {
        IntegrationDraftRequest.Settings settings = mock(IntegrationDraftRequest.Settings.class);
        when(settings.getAuthenticationMethod()).thenReturn("authMethod");
        when(settings.getSyncType()).thenReturn("syncType");
        when(settings.getSyncSchedule()).thenReturn("schedule");
        when(settings.getWeekDay()).thenReturn("Monday");
        when(settings.getSyncTime()).thenReturn("10:00");
        when(settings.getIdentifiersList()).thenReturn(Arrays.asList());

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getSettings()).thenReturn(settings);
        when(request.getPageName()).thenReturn("pageName");

        doNothing().when(spyDao).saveSettingsConfigurationAdditionalFields(any(), any());
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.saveSettings("pk", "sk", "status", request));
        verify(spyDao, times(1)).updateItem(any(), any(), any(), any(), any());
        verify(spyDao, times(1)).saveSettingsConfigurationAdditionalFields(eq("pk"), eq(Arrays.asList()));
    }

    @Test
    void saveSettings_shouldHandleDynamoDbException() {
        IntegrationDraftRequest.Settings settings = mock(IntegrationDraftRequest.Settings.class);
        when(settings.getAuthenticationMethod()).thenReturn("authMethod");
        when(settings.getSyncType()).thenReturn("syncType");
        when(settings.getSyncSchedule()).thenReturn("schedule");
        when(settings.getWeekDay()).thenReturn("Monday");
        when(settings.getSyncTime()).thenReturn("10:00");
        when(settings.getIdentifiersList()).thenReturn(null);

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getSettings()).thenReturn(settings);
        when(request.getPageName()).thenReturn("pageName");

        // Set up security context to avoid NPE
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        doThrow(DynamoDbException.builder().message("error").build())
                .when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertThrows(DynamoDbException.class, () -> spyDao.saveSettings("pk", "sk", "status", request));
    }

    @Test
    void saveCoreConfigurationAdditionalFields_shouldHandleNullAndEmptyFields() {
        assertDoesNotThrow(() -> integrationDao.saveCoreConfigurationAdditionalFields("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveCoreConfigurationAdditionalFields("pk", Arrays.asList()));
    }

    @Test
    void saveCoreConfigurationAdditionalFields_shouldSkipNullFieldNameOrValue() {
        List<IntegrationDraftRequest.FieldEntry> fields = Arrays.asList(
                new IntegrationDraftRequest.FieldEntry(null, "value1"),
                new IntegrationDraftRequest.FieldEntry("field2", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveCoreConfigurationAdditionalFields("pk", fields));
    }

    @Test
    void saveSettingsConfigurationAdditionalFields_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveSettingsConfigurationAdditionalFields("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveSettingsConfigurationAdditionalFields("pk", Arrays.asList()));
    }

    @Test
    void saveSettingsConfigurationAdditionalFields_shouldSkipNullThirdPartyIdentifier() {
        List<IntegrationDraftRequest.UniqueIdentifiers> list = Arrays.asList(
                new IntegrationDraftRequest.UniqueIdentifiers(null, "ssId")
        );
        assertDoesNotThrow(() -> integrationDao.saveSettingsConfigurationAdditionalFields("pk", list));
    }

    @Test
    void saveMetaDataMapping_shouldUpdateItemAndSaveMetaDataFields() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        IntegrationDraftRequest.MetaDataMappings mapping = new IntegrationDraftRequest.MetaDataMappings("meta1", "field1");
        metaData.setPrefix("PRE");
        metaData.setMetaDataMappings(List.of(mapping));

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getMetaData()).thenReturn(metaData);
        when(request.getPageName()).thenReturn("metadataPage");

        doNothing().when(spyDao).saveMetaDataFieldMapping(any(), any());
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.saveMetaDataMapping("pk", "sk", "status", request));
        verify(spyDao, times(1)).updateItem(any(), any(), any(), any(), any());
        verify(spyDao, times(1)).saveMetaDataFieldMapping(eq("pk"), eq(List.of(mapping)));
    }

    @Test
    void saveMetaDataFieldMapping_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveMetaDataFieldMapping("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveMetaDataFieldMapping("pk", List.of()));
    }

    @Test
    void saveMetaDataFieldMapping_shouldSkipNullFields() {
        List<IntegrationDraftRequest.MetaDataMappings> mappings = List.of(
                new IntegrationDraftRequest.MetaDataMappings(null, "field1"),
                new IntegrationDraftRequest.MetaDataMappings("meta2", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveMetaDataFieldMapping("pk", mappings));
    }

    @Test
    void saveContentMapping_shouldUpdateItemAndSaveMappings() {
        IntegrationDraftRequest.ContentMapping contentMapping = new IntegrationDraftRequest.ContentMapping();
        IntegrationDraftRequest.ContentTypeMapping ctm = new IntegrationDraftRequest.ContentTypeMapping("Video", "SkillSpringVideo");
        IntegrationDraftRequest.CategoryTypeMapping catm = new IntegrationDraftRequest.CategoryTypeMapping("TypeA", "TypeB");
        IntegrationDraftRequest.CompletionSyncMapping csm = new IntegrationDraftRequest.CompletionSyncMapping("Completed", "Done");
        contentMapping.setContentTypeMapping(List.of(ctm));
        contentMapping.setCategoryMappingType("TypeA");
        contentMapping.setCategoryName("Category1");
        contentMapping.setCategoryTypeMapping(List.of(catm));
        contentMapping.setCompletionSyncMapping(List.of(csm));

        IntegrationDraftRequest request = mock(IntegrationDraftRequest.class);
        when(request.getContentMapping()).thenReturn(contentMapping);
        when(request.getPageName()).thenReturn("contentPage");

        doNothing().when(spyDao).saveContentTypeMapping(any(), any());
        doNothing().when(spyDao).saveCategoryTypeMapping(any(), any());
        doNothing().when(spyDao).saveCompletionSyncMapping(any(), any());
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.saveContentMapping("pk", "sk", "status", request));
        verify(spyDao, times(1)).updateItem(any(), any(), any(), any(), any());
        verify(spyDao, times(1)).saveContentTypeMapping(eq("pk"), eq(List.of(ctm)));
        verify(spyDao, times(1)).saveCategoryTypeMapping(eq("pk"), eq(List.of(catm)));
        verify(spyDao, times(1)).saveCompletionSyncMapping(eq("pk"), eq(List.of(csm)));
    }

    @Test
    void saveContentTypeMapping_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveContentTypeMapping("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveContentTypeMapping("pk", List.of()));
    }

    @Test
    void saveContentTypeMapping_shouldSkipNullFields() {
        List<IntegrationDraftRequest.ContentTypeMapping> mappings = List.of(
                new IntegrationDraftRequest.ContentTypeMapping(null, "SkillSpringVideo"),
                new IntegrationDraftRequest.ContentTypeMapping("Video", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveContentTypeMapping("pk", mappings));
    }

    @Test
    void saveCategoryTypeMapping_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveCategoryTypeMapping("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveCategoryTypeMapping("pk", List.of()));
    }

    @Test
    void saveCategoryTypeMapping_shouldSkipNullFields() {
        List<IntegrationDraftRequest.CategoryTypeMapping> mappings = List.of(
                new IntegrationDraftRequest.CategoryTypeMapping(null, "SkillSpringCategory"),
                new IntegrationDraftRequest.CategoryTypeMapping("Category", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveCategoryTypeMapping("pk", mappings));
    }

    @Test
    void saveCompletionSyncMapping_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveCompletionSyncMapping("pk", null));
        assertDoesNotThrow(() -> integrationDao.saveCompletionSyncMapping("pk", List.of()));
    }

    @Test
    void saveCompletionSyncMapping_shouldSkipNullFields() {
        List<IntegrationDraftRequest.CompletionSyncMapping> mappings = List.of(
                new IntegrationDraftRequest.CompletionSyncMapping(null, "SkillSpringStatus"),
                new IntegrationDraftRequest.CompletionSyncMapping("Completed", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveCompletionSyncMapping("pk", mappings));
    }

    @Test
    void testSavesSFTPConfiguration(){

        // Mock AuthUser as principal
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        SFTPIntegrationReqDto.Configuration sftpIntegrationReqDto = mock(SFTPIntegrationReqDto.Configuration.class);
        when(sftpIntegrationReqDto.getProvider()).thenReturn("Test Integration");
        when(sftpIntegrationReqDto.getSyncType()).thenReturn("Full");
        when(sftpIntegrationReqDto.getLocation()).thenReturn("/test/location");
        when(sftpIntegrationReqDto.getUserName()).thenReturn("testuser");
        when(sftpIntegrationReqDto.getPassword()).thenReturn("password");
        when(sftpIntegrationReqDto.getHost()).thenReturn("host");
        when(sftpIntegrationReqDto.getPort()).thenReturn("22");
        when(sftpIntegrationReqDto.getTestConnection()).thenReturn("true");

        SFTPIntegrationReqDto sftpIntegrationReqDto1 = mock(SFTPIntegrationReqDto.class);
        when(sftpIntegrationReqDto1.getConfiguration()).thenReturn(sftpIntegrationReqDto);
        when(sftpIntegrationReqDto1.getLmsIntegrationId()).thenReturn("lmsId");
        when(sftpIntegrationReqDto1.getIntegrationType()).thenReturn("SFTP");
        when(sftpIntegrationReqDto1.getAction()).thenReturn("Create");
        when(sftpIntegrationReqDto1.getStatus()).thenReturn("Draft");

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().item(new HashMap<>()).build());

        assertDoesNotThrow(() -> integrationDao.saveSFTPConfiguration("pk", "sk", "type", "status", sftpIntegrationReqDto1));


    }
    // Test case for saveSftpSettings method
    @Test
    void saveSftpSettings_shouldUpdateItemSuccessfully() {
        // Mock the SftpSettingsDTO
        SFTPIntegrationReqDto.SftpSettingsDTO settings = mock(SFTPIntegrationReqDto.SftpSettingsDTO.class);
        when(settings.getSyncTime()).thenReturn("10:00");

        // Mock the request object
        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getSftpSettingsDTO()).thenReturn(settings);
        when(request.getPageName()).thenReturn("SFTP_SETTINGS_PAGE");

        // Set up security context to avoid NPE
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock the updateItem call
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        // Call the method
        assertDoesNotThrow(() -> spyDao.saveSftpSettings("pk", "sk", "ACTIVE", request));

        // Verify the updateItem method was called with the correct parameters
        verify(spyDao, times(1)).updateItem(eq("pk"), eq("sk"), any(), any(), any());
    }

    // Test case for successful retrieval of SFTP integration by partition key
    @Test
    void getSFTPIntegrationByPartitionKey_returnsIntegrationResponse() {
        String partitionKey = "testPartitionKey";

        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("type", AttributeValue.fromS(Constants.SFTP_CATEGORY_TYPE_MAPPING_TYPE));
        item1.put("thirdPartyCategoryType", AttributeValue.fromS("CategoryA"));
        item1.put("skillSpringCategoryType", AttributeValue.fromS("CategoryB"));

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("type", AttributeValue.fromS("integration"));
        item2.put("provider", AttributeValue.fromS("TestProvider"));

        QueryResponse response = QueryResponse.builder()
            .count(2)
            .items(item1, item2)
            .build();

        try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
            SFTPIntegrationResponseDto categoryMappingDto = new SFTPIntegrationResponseDto();
            categoryMappingDto.setThirdPartyCategoryType("CategoryA");
            categoryMappingDto.setSkillSpringCategoryType("CategoryB");

            SFTPIntegrationResponseDto integrationDto = new SFTPIntegrationResponseDto();
            integrationDto.setProvider("TestProvider");

            util.when(() -> CommonUtil.mapItemToDto(item1, SFTPIntegrationResponseDto.class)).thenReturn(categoryMappingDto);
            util.when(() -> CommonUtil.mapItemToDto(item2, SFTPIntegrationResponseDto.class)).thenReturn(integrationDto);

            when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

            SFTPIntegrationResponseDto result = integrationDao.getSFTPIntegrationByPartitionKey(partitionKey, false);

            assertNotNull(result);
            assertEquals("TestProvider", result.getProvider());
            assertEquals(1, result.getSftpCategoryTypeMapping().size());
            assertEquals("CategoryA", result.getSftpCategoryTypeMapping().get(0).getThirdPartyCategoryType());
            assertEquals("CategoryB", result.getSftpCategoryTypeMapping().get(0).getSkillSpringCategoryType());
        }
    }

    // Test case for no results found
    @Test
    void getSFTPIntegrationByPartitionKey_returnsNullIfNotFound() {
        String partitionKey = "testPartitionKey";
        QueryResponse response = QueryResponse.builder().count(0).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);
        SFTPIntegrationResponseDto result = integrationDao.getSFTPIntegrationByPartitionKey(partitionKey, false);
        assertNull(result);
    }

    // Test case for DynamoDbException
    @Test
    void getSFTPIntegrationByPartitionKey_throwsDataBaseExceptionOnError() {
        String partitionKey = "testPartitionKey";
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("error").build());
        assertThrows(DataBaseException.class, () -> integrationDao.getSFTPIntegrationByPartitionKey(partitionKey, false));
    }

    @Test
    void saveSftpCategoryTypeMapping_shouldSaveValidMappings() {
        List<SFTPIntegrationReqDto.SftpCategoryTypeMapping> mappings = List.of(
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping("ThirdPartyCategory1", "SkillSpringCategory1"),
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping("ThirdPartyCategory2", "SkillSpringCategory2")
        );
        assertDoesNotThrow(() -> integrationDao.saveSftpCategoryTypeMapping("pk", mappings));
        verify(dynamoDbClient, times(2)).putItem(any(PutItemRequest.class));
    }

    @Test
    void saveSftpCategoryTypeMapping_shouldSkipNullFields() {
        List<SFTPIntegrationReqDto.SftpCategoryTypeMapping> mappings = List.of(
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping(null, "SkillSpringCategory1"),
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping("ThirdPartyCategory2", null)
        );
        assertDoesNotThrow(() -> integrationDao.saveSftpCategoryTypeMapping("pk", mappings));
        verify(dynamoDbClient, times(0)).putItem(any(PutItemRequest.class));
    }

    @Test
    void saveSftpCategoryTypeMapping_shouldHandleNullAndEmptyList() {
        assertDoesNotThrow(() -> integrationDao.saveSftpCategoryTypeMapping("pk", null));
        verify(dynamoDbClient, times(0)).putItem(any(PutItemRequest.class));
        assertDoesNotThrow(() -> integrationDao.saveSftpCategoryTypeMapping("pk", List.of()));
        verify(dynamoDbClient, times(0)).putItem(any(PutItemRequest.class));
    }

    // Test case for successful execution of saveSftpCategoryMapping
    @Test
    void saveSftpCategoryMapping_shouldUpdateItemAndSaveMappings() {
        // Mock the CategoryMapping object
        SFTPIntegrationReqDto.CategoryMapping categoryMapping = mock(SFTPIntegrationReqDto.CategoryMapping.class);
        List<SFTPIntegrationReqDto.SftpCategoryTypeMapping> mappings = List.of(
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping("ThirdPartyCategory1", "SkillSpringCategory1"),
            new SFTPIntegrationReqDto.SftpCategoryTypeMapping("ThirdPartyCategory2", "SkillSpringCategory2")
        );
        when(categoryMapping.getCategoryTypeMappings()).thenReturn(mappings);

        // Mock the request object
        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getCategoryMapping()).thenReturn(categoryMapping);
        when(request.getPageName()).thenReturn("CATEGORY_MAPPING_PAGE");

        // Set up security context to avoid NPE
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock the updateItem and saveSftpCategoryTypeMapping calls
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());
        doNothing().when(spyDao).saveSftpCategoryTypeMapping(any(), any());

        // Call the method
        assertDoesNotThrow(() -> spyDao.saveSftpCategoryMapping("pk", "sk", "ACTIVE", request));

        // Verify the updateItem method was called with the correct parameters
        verify(spyDao, times(1)).updateItem(eq("pk"), eq("sk"), any(), any(), any());
        verify(spyDao, times(1)).saveSftpCategoryTypeMapping(eq("pk"), eq(mappings));
    }

    // Test case for handling null category type mappings
    @Test
    void saveSftpCategoryMapping_shouldHandleNullCategoryTypeMappings() {
        // Mock the CategoryMapping object
        SFTPIntegrationReqDto.CategoryMapping categoryMapping = mock(SFTPIntegrationReqDto.CategoryMapping.class);
        when(categoryMapping.getCategoryTypeMappings()).thenReturn(null);

        // Mock the request object
        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getCategoryMapping()).thenReturn(categoryMapping);
        when(request.getPageName()).thenReturn("CATEGORY_MAPPING_PAGE");

        // Mock the updateItem call
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        // Call the method
        assertDoesNotThrow(() -> spyDao.saveSftpCategoryMapping("pk", "sk", "ACTIVE", request));

        // Verify the updateItem method was called
        verify(spyDao, times(1)).updateItem(eq("pk"), eq("sk"), any(), any(), any());
        verify(spyDao, times(1)).saveSftpCategoryTypeMapping(eq("pk"), eq(null));
    }

    // Test case for handling empty category type mappings
    @Test
    void saveSftpCategoryMapping_shouldHandleEmptyCategoryTypeMappings() {
        // Mock the CategoryMapping object
        SFTPIntegrationReqDto.CategoryMapping categoryMapping = mock(SFTPIntegrationReqDto.CategoryMapping.class);
        when(categoryMapping.getCategoryTypeMappings()).thenReturn(List.of());

        // Mock the request object
        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getCategoryMapping()).thenReturn(categoryMapping);
        when(request.getPageName()).thenReturn("CATEGORY_MAPPING_PAGE");

        // Mock the updateItem call
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        // Call the method
        assertDoesNotThrow(() -> spyDao.saveSftpCategoryMapping("pk", "sk", "ACTIVE", request));

        // Verify the updateItem method was called
        verify(spyDao, times(1)).updateItem(eq("pk"), eq("sk"), any(), any(), any());
        verify(spyDao, times(1)).saveSftpCategoryTypeMapping(eq("pk"), eq(List.of()));
    }

    // Test case for handling DynamoDbException
    @Test
    void saveSftpCategoryMapping_shouldThrowDataBaseExceptionOnDynamoDbError() {
        // Mock the CategoryMapping object
        SFTPIntegrationReqDto.CategoryMapping categoryMapping = mock(SFTPIntegrationReqDto.CategoryMapping.class);
        when(categoryMapping.getCategoryTypeMappings()).thenReturn(List.of());

        // Mock the request object
        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getCategoryMapping()).thenReturn(categoryMapping);
        when(request.getPageName()).thenReturn("CATEGORY_MAPPING_PAGE");

        // Mock the updateItem call to throw an exception
        doThrow(DynamoDbException.builder().message("error").build())
            .when(spyDao).updateItem(any(), any(), any(), any(), any());

        // Call the method and assert exception
        assertThrows(DynamoDbException.class, () -> spyDao.saveSftpCategoryMapping("pk", "sk", "ACTIVE", request));
    }

    @Test
    void fetchExistingIntegration_returnsExpectedResponse() {
        String sortKey = "createdOn";
        String status = "ACTIVE";
        String integrationType = "SFTP";

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("provider", AttributeValue.fromS("TestProvider"));
        item.put("uniqIntegrationKey", AttributeValue.fromS("uniqueKey"));
        item.put("type", AttributeValue.fromS(Constants.INTEGRATION_TYPE));
        List<Map<String, AttributeValue>> items = List.of(item);

        QueryResponse queryResponse = QueryResponse.builder()
                .items(items)
                .lastEvaluatedKey(Map.of("pk", AttributeValue.fromS("pkValue")))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        IntegrationListResponse response = integrationDao.fetchExistingIntegration(sortKey, status, integrationType);

        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertEquals("TestProvider", response.getIntegrationList().get(0).getProvider());
        assertEquals("uniqueKey", response.getIntegrationList().get(0).getUniqIntegrationKey());
        assertEquals(Map.of("pk", AttributeValue.fromS("pkValue")), response.getLastEvaluatedKey());
        assertEquals(1, response.getCount());
    }

    @Test
    void fetchExistingIntegration_throwsDataBaseExceptionOnDynamoDbError() {
        String sortKey = "createdOn";
        String status = "ACTIVE";
        String integrationType = "SFTP";
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("error").build());

        DataBaseException ex = assertThrows(DataBaseException.class, () ->
                integrationDao.fetchExistingIntegration(sortKey, status, integrationType)
        );
        assertTrue(ex.getMessage().contains("Error reading from Integrations table in DynamoDB"));
    }

    @Test
    void updateSFTPConfiguration_shouldUpdateItemWithCorrectValues() {
        String pk = "pkTest";
        String sk = "skTest";
        String status = "ACTIVE";

        SFTPIntegrationReqDto.Configuration config = mock(SFTPIntegrationReqDto.Configuration.class);
        when(config.getProvider()).thenReturn("TestProvider");

        SFTPIntegrationReqDto request = mock(SFTPIntegrationReqDto.class);
        when(request.getReasonForChange()).thenReturn("Reason");
        when(request.getConfiguration()).thenReturn(config);
        when(request.getStatus()).thenReturn(status);

        AuthUser authUser = mock(AuthUser.class);
        when(authUser.getUsername()).thenReturn("testUser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        IntegrationDaoImpl spyDao = Mockito.spy(integrationDao);
        doNothing().when(spyDao).updateItem(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> spyDao.updateSFTPConfiguration(pk, sk, status, request));
        verify(spyDao, times(1)).updateItem(eq(pk), eq(sk), any(), any(), any());
    }

}
