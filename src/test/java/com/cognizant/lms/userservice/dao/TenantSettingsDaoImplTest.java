package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantSettingsDaoImplTest {

    private DynamoDbClient dynamoDbClient;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<Tenant> tenantTable;
    private DynamoDBConfig dynamoDBConfig;
    private UserService userService;
    private TenantSettingsDaoImpl dao;
    private final String tableName = "Tenants";

    @BeforeEach
    void setup() {
        dynamoDbClient = mock(DynamoDbClient.class);
        enhancedClient = mock(DynamoDbEnhancedClient.class);
        tenantTable = mock(DynamoDbTable.class);
        dynamoDBConfig = mock(DynamoDBConfig.class);
        userService = mock(UserService.class);

        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(enhancedClient);
        when(enhancedClient.table(eq(tableName), any(TableSchema.class))).thenReturn(tenantTable);

        dao = new TenantSettingsDaoImpl(dynamoDbClient, dynamoDBConfig, tableName, userService);
    }

    @Test
    void testGetTenantReturnsMappedResponse() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");

            Map<String, AttributeValue> item = Map.of(
                "type", AttributeValue.builder().s("settings").build(),
                "reviewEmail", AttributeValue.builder().s("demo@example.com").build(),
                "courseReviewCommentType", AttributeValue.builder().s("Auto").build(),
                "settingName", AttributeValue.builder().s("review").build(),
                "createdOn", AttributeValue.builder().s("2025-01-01").build(),
                "createdBy", AttributeValue.builder().s("test-user").build(),
                "updatedBy", AttributeValue.builder().s("admin").build(),
                "updatedDate", AttributeValue.builder().s("2025-06-21").build()
            );

            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());

            TenantSettingsResponse res = dao.getTenant("review");
            assertNotNull(res);
            assertEquals("demo@example.com", res.getReviewEmail());
            assertEquals("Auto", res.getCourseReviewCommentType());
            assertEquals("admin", res.getUpdatedBy());
        }
    }

    @Test
    void testGetTenantReturnsEmptyOnNoMatch() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("notfound");

            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());

            assertNull(dao.getTenant("missing"));
        }
    }

    @Test
    void testGetTenantHandlesMissingOptionalFields() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");

            Map<String, AttributeValue> item = Map.of(
                "type", AttributeValue.builder().s("settings").build(),
                "reviewEmail", AttributeValue.builder().s("test@example.com").build(),
                "courseReviewCommentType", AttributeValue.builder().s("Manual").build(),
                "settingName", AttributeValue.builder().s("config").build(),
                "createdOn", AttributeValue.builder().s("2025-05-01").build(),
                "createdBy", AttributeValue.builder().s("creator").build()
            );

            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());

            TenantSettingsResponse res = dao.getTenant("config");
            assertNotNull(res);
            assertEquals("", res.getUpdatedBy());
            assertEquals("", res.getUpdatedDate());
        }
    }

    @Test
    void testGetTenantWithUnknownFieldLogsError() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");

            Map<String, AttributeValue> item = Map.of(
                "nonExistentField", AttributeValue.builder().s("value").build(),
                "type", AttributeValue.builder().s("settings").build()
            );

            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());

            TenantSettingsResponse res = dao.getTenant("bogus");
            assertNotNull(res);
        }
    }

    @Test
    void testGetTenantThrowsDatabaseExceptionOnQueryError() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");

            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Query Failed").build());

            assertThrows(DataBaseException.class, () -> dao.getTenant("fail"));
        }
    }

    @Test
    void testUpdateTenantSetingsUpdatesSuccessfully() {
        Tenant tenant = new Tenant();
        tenant.setPk("demo");
        tenant.setSk("demo#settings");
        tenant.setReviewEmail("update@example.com");
        tenant.setCourseReviewCommentType("Auto");

        SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        UpdateItemResponse mockResponse = mock(UpdateItemResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(mockResponse);

        boolean result = dao.updateTenantSetings(tenant, "updater", "2025-06-20");
        assertTrue(result);
    }

    @Test
    void testUpdateTenantSetingsThrowsDatabaseException() {
        Tenant tenant = new Tenant();
        tenant.setPk("fail");
        tenant.setSk("fail#sk");

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DDB Error").build());

        assertThrows(DataBaseException.class,
            () -> dao.updateTenantSetings(tenant, "who", "2025-06-22"));
    }

    @Test
    void testCreateTenantSettingsSuccess() {
        Tenant tenant = new Tenant();
        doNothing().when(tenantTable).putItem(tenant);

        boolean result = dao.createTenantSettings(tenant);
        assertTrue(result);
    }

    @Test
    void testCreateTenantSettingsThrowsDatabaseException() {
        Tenant tenant = new Tenant();
        doThrow(DynamoDbException.builder().message("Insert failed").build())
            .when(tenantTable).putItem(tenant);

        assertThrows(DataBaseException.class, () -> dao.createTenantSettings(tenant));
    }

    @Test
    void testGetTenantFeatureFlagsSuccess() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            Map<String, AttributeValue> item = Map.of(
                "pk", AttributeValue.builder().s("demo").build(),
                "sk", AttributeValue.builder().s("demo#featureFlags").build(),
                "name", AttributeValue.builder().s("featureFlags").build(),
                "featureFlags", AttributeValue.builder().s("{\"aiAssistant\":true,\"learningPaths\":false}").build()
            );
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());
            TenantFeatureFlagsDto dto = dao.getTenantFeatureFlags("cognizant");
            assertNotNull(dto);
            assertEquals("demo", dto.getPk());
            assertEquals("featureFlags", dto.getName());
            assertEquals(Boolean.TRUE, dto.getFeatureFlags().get("aiAssistant"));
            assertEquals(Boolean.FALSE, dto.getFeatureFlags().get("learningPaths"));
        }
    }

    @Test
    void testGetTenantFeatureFlagsEmptyResponseReturnsNull() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of()).build());
            assertNull(dao.getTenantFeatureFlags("cognizant"));
        }
    }

    @Test
    void testGetTenantFeatureFlagsMissingFeatureFlags() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            Map<String, AttributeValue> item = Map.of(
                "pk", AttributeValue.builder().s("demo").build(),
                "sk", AttributeValue.builder().s("demo#featureFlags").build(),
                "name", AttributeValue.builder().s("featureFlags").build()
            );
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());
            TenantFeatureFlagsDto dto = dao.getTenantFeatureFlags("cognizant");
            assertNotNull(dto);
            assertNull(dto.getFeatureFlags());
        }
    }

    @Test
    void testGetTenantFeatureFlagsInvalidJsonFallsBackToEmptyMap() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            Map<String, AttributeValue> item = Map.of(
                "pk", AttributeValue.builder().s("demo").build(),
                "sk", AttributeValue.builder().s("demo#featureFlags").build(),
                "name", AttributeValue.builder().s("featureFlags").build(),
                "featureFlags", AttributeValue.builder().s("{invalid json").build()
            );
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());
            TenantFeatureFlagsDto dto = dao.getTenantFeatureFlags("cognizant");
            assertNotNull(dto);
            assertNotNull(dto.getFeatureFlags());
            assertTrue(dto.getFeatureFlags().isEmpty());
        }
    }

    @Test
    void testGetTenantFeatureFlagsThrowsDatabaseExceptionOnDynamoError() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Query Failed").build());
            assertThrows(DataBaseException.class, () -> dao.getTenantFeatureFlags("cognizant"));
        }
    }

    @Test
    void testGetTenantFeatureFlagsThrowsDatabaseExceptionOnGeneralError() {
        try (MockedStatic<TenantUtil> mockUtil = Mockito.mockStatic(TenantUtil.class)) {
            mockUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
            // Missing pk causes NullPointerException in mapFeatureFlags
            Map<String, AttributeValue> item = Map.of(
                "sk", AttributeValue.builder().s("demo#featureFlags").build(),
                "name", AttributeValue.builder().s("featureFlags").build(),
                "featureFlags", AttributeValue.builder().s("{\"aiAssistant\":true}").build()
            );
            when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());
            assertThrows(DataBaseException.class, () -> dao.getTenantFeatureFlags("cognizant"));
        }
    }
}
