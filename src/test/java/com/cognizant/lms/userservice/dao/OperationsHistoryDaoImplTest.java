package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationsHistoryDaoImplTest {
    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbTable<OperationsHistory> operationsHistoryTable;

    private OperationsHistoryDaoImpl operationsHistoryDaoImpl;

    @BeforeEach
    public void setUp() {
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(dynamoDbEnhancedClient);
        when(dynamoDbEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(operationsHistoryTable);

        operationsHistoryDaoImpl = new OperationsHistoryDaoImpl(dynamoDBConfig, "testTableName");
    }

    @Test
    void testSaveLogFileData() {
        doNothing().when(operationsHistoryTable).putItem(any(OperationsHistory.class));

        OperationsHistory logFile = new OperationsHistory();
        operationsHistoryDaoImpl.saveLogFileData(logFile);
        verify(operationsHistoryTable, times(1)).putItem(logFile);
    }
}