package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSettingsDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;
    @Mock
    private DynamoDbClient dynamoDbClient;
    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    @Mock
    private DynamoDbTable<UserSettings> userTable;

    private UserSettingsDaoImpl dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(enhancedClient);
        when(enhancedClient.table(anyString(), ArgumentMatchers.<TableSchema<UserSettings>>any())).thenReturn(userTable);
        dao = new UserSettingsDaoImpl(dynamoDBConfig, "test-table");
    }

    @Test
    void getUserSettingsByEmailId_found() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("pk1").build());
        item.put("sk", AttributeValue.builder().s("sk1").build());
        item.put("emailId", AttributeValue.builder().s("test@example.com").build());

        QueryResponse response = QueryResponse.builder().items(item).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        UserSettings result = dao.getUserSettingsByEmailId("test@example.com", "sk1");
        assertNotNull(result);
        assertEquals("pk1", result.getPk());
        assertEquals("sk1", result.getSk());
        assertEquals("test@example.com", result.getEmailId());
    }

    @Test
    void getUserSettingsByEmailId_notFound() {
        QueryResponse response = QueryResponse.builder().items(Collections.emptyList()).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        UserSettings result = dao.getUserSettingsByEmailId("notfound@example.com", "sk1");
        assertNull(result);
    }

    @Test
    void saveUserSettings_callsPutItem() {
        UserSettings settings = new UserSettings();
        dao.saveUserSettings(settings);
        verify(userTable, times(1)).putItem(settings);
    }

    @Test
    void updateEmptyTypeFields_withItemsAndLastEvaluatedKey() {
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("pk", AttributeValue.builder().s("pk1").build());
        item1.put("sk", AttributeValue.builder().s("sk1").build());
        item1.put("emailId", AttributeValue.builder().s("test@example.com").build());

        List<Map<String, AttributeValue>> items = List.of(item1);

        Map<String, AttributeValue> lastEvaluatedKey = Map.of("pk", AttributeValue.builder().s("pk1").build());
        ScanResponse scanResponse = ScanResponse.builder()
                .items(items)
                .lastEvaluatedKey(lastEvaluatedKey)
                .build();

        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);
        when(dynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class)))
                .thenReturn(BatchWriteItemResponse.builder().build());

        Map<String, String> lastEvaluatedKeyString = Map.of("pk", "pk1");
        Map<String, AttributeValue> result = dao.updateEmptyTypeFields(lastEvaluatedKeyString, 10);

        assertNotNull(result);
        assertEquals("pk1", result.get("pk").s());
    }

    @Test
    void updateEmptyTypeFields_noItems_returnsNullKey() {
        ScanResponse scanResponse = ScanResponse.builder()
                .items(Collections.emptyList())
                .lastEvaluatedKey(Collections.emptyMap())
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        Map<String, AttributeValue> result = dao.updateEmptyTypeFields(null, 10);
        assertTrue(result == null || result.isEmpty());
    }
}