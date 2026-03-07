package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.LogFileResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OperationsHistoryFilterSortDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private QueryResponse queryResponse;

    private OperationsHistoryFilterSortDaoImpl operationsHistoryFilterSortDao;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        operationsHistoryFilterSortDao = new OperationsHistoryFilterSortDaoImpl(dynamoDBConfig, "tableName", "partitionKey");
    }

    @Test
    public void testGetLogFileLists_Success() {
        String partitionKeyValue = "pkValue";
        String sortKey = "timestamp";
        String order = "asc";
        String process = "process1";
        String userEmail = "user@example.com";
        int perPage = 10;
        Map<String, String> lastEvaluatedKey = new HashMap<>();
        lastEvaluatedKey.put("key", "value");

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("operation", AttributeValue.builder().s("process1").build());
        item.put("email", AttributeValue.builder().s("user@example.com").build());

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(queryResponse.items()).thenReturn(List.of(item));

        LogFileResponse response = operationsHistoryFilterSortDao.getLogFileLists(
                partitionKeyValue , sortKey, order, process, userEmail, lastEvaluatedKey,perPage);

        assertNotNull(response);
        assertEquals(1, response.getLogFiles().size());
        assertEquals("process1", response.getLogFiles().get(0).getOperation());
        assertEquals("user@example.com", response.getLogFiles().get(0).getEmail());
    }

    @Test
    public void testGetLogFileLists_DatabaseException() {
        String partitionKeyValue = "pkValue";
        String sortKey = "timestamp";
        String order = "asc";
        String process = "process1";
        String userEmail = "user@example.com";
        int perPage = 10;
        Map<String, String> lastEvaluatedKey = new HashMap<>();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.class);

        assertThrows(DataBaseException.class, () -> {
            operationsHistoryFilterSortDao.getLogFileLists(
                    partitionKeyValue, sortKey, order, process, userEmail, lastEvaluatedKey, perPage);
        });
    }
}