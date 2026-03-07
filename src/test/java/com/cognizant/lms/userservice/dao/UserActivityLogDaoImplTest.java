package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.UserActivityLogDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserActivityLogDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;
    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    @Mock
    private DynamoDbTable<UserActivityLogDto> table;
    @Mock
    private DynamoDbIndex<UserActivityLogDto> index;
    @Mock
    private PageIterable<UserActivityLogDto> pageIterable;

    @InjectMocks
    private UserActivityLogDaoImpl dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dao = new UserActivityLogDaoImpl(dynamoDBConfig, "testTable");
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(enhancedClient);
        when(enhancedClient.table(eq("testTable"), any(TableSchema.class))).thenReturn(table);
    }

    @Test
    void saveUserActivityLog_success() {
        UserActivityLogDto dto = new UserActivityLogDto();
        dao.saveUserActivityLog(dto);
        verify(table, times(1)).putItem(dto);
    }

    @Test
    void saveUserActivityLog_dynamoDbException() {
        UserActivityLogDto dto = new UserActivityLogDto();
        doThrow(DynamoDbException.builder().message("error").build()).when(table).putItem(any(UserActivityLogDto.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.saveUserActivityLog(dto));
        assertTrue(ex.getMessage().contains("Failed to log user activity"));
    }

    @Test
    void findByTimestamp_success() {
        when(table.index(anyString())).thenReturn(index);
        when(index.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.stream()).thenReturn(List.of(
                Page.create(List.of(new UserActivityLogDto()))
        ).stream());
        List<UserActivityLogDto> result = dao.findByTimestamp("pk", "timestamp");
        assertEquals(1, result.size());
    }

    @Test
    void findByTimestamp_dynamoDbException() {
        when(table.index(anyString())).thenReturn(index);
        when(index.query(any(QueryConditional.class))).thenThrow(DynamoDbException.builder().message("error").build());
        assertThrows(RuntimeException.class, () -> dao.findByTimestamp("pk", "timestamp"));
    }
}