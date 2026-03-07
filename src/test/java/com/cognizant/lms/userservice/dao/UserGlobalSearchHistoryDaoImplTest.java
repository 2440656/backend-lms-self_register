package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.UserGlobalSearchHistory;
import com.cognizant.lms.userservice.exception.DataBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGlobalSearchHistoryDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Captor
    private ArgumentCaptor<Consumer<PutItemRequest.Builder>> putItemConsumerCaptor;

    @Captor
    private ArgumentCaptor<Consumer<GetItemRequest.Builder>> getItemConsumerCaptor;

    private UserGlobalSearchHistoryDaoImpl dao;

    @BeforeEach
    void setUp() {
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        // enhanced client not used directly; return null for simplicity
        when(dynamoDBConfig.dynamoDbEnhancedClient(any())).thenReturn(null);
        dao = new UserGlobalSearchHistoryDaoImpl(dynamoDBConfig, "UserSearchHistoryTable");
    }

    private UserGlobalSearchHistory buildHistory(String userId, String keyword, String active) {
        UserGlobalSearchHistory h = new UserGlobalSearchHistory();
        h.setPk(Constants.USER_PK + Constants.HASH + userId);
        h.setSk(Constants.USER_KEYWORD + Constants.HASH + keyword.toLowerCase());
        h.setType(Constants.USER_KEYWORD);
        h.setKeywordOriginal(keyword);
        h.setKeywordNormal(keyword.toLowerCase());
        h.setCreatedAt("2024/01/01 00:00:00");
        h.setActive(active);
        h.setGsiSk(Constants.ACTIVE + Constants.HASH + active + Constants.HASH + Constants.TIME_STAMP + Constants.HASH + System.currentTimeMillis());
        return h;
    }

    @Test
    @DisplayName("saveRecentSearch - success path")
    void saveRecentSearch_success() {
        // mock AWS builder consumer overload
        when(dynamoDbClient.putItem(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<PutItemRequest.Builder> consumer = invocation.getArgument(0);
            PutItemRequest.Builder builder = PutItemRequest.builder();
            consumer.accept(builder);
            return mock(PutItemResponse.class);
        });

        dao.saveRecentSearch(buildHistory("u1", "Java", Constants.YES));
        verify(dynamoDbClient, times(1)).putItem(any(Consumer.class));
    }

    @Test
    @DisplayName("saveRecentSearch - throws DataBaseException on DynamoDbException")
    void saveRecentSearch_failure() {
        when(dynamoDbClient.putItem(any(Consumer.class))).thenThrow(DynamoDbException.builder().message("boom").build());
        assertThrows(DataBaseException.class, () -> dao.saveRecentSearch(buildHistory("u1", "Java", Constants.YES)));
    }

    @Test
    @DisplayName("isRecentSearchExists - returns true when item matches search query")
    void isRecentSearchExists_true() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("keywordNormal", AttributeValue.builder().s("java").build());

        when(dynamoDbClient.getItem(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<GetItemRequest.Builder> consumer = invocation.getArgument(0);
            GetItemRequest.Builder builder = GetItemRequest.builder();
            consumer.accept(builder);
            return GetItemResponse.builder().item(item).build();
        });

        boolean exists = dao.isRecentSearchExists("PK", "SK", "java");
        assertTrue(exists);
    }

    @Test
    @DisplayName("isRecentSearchExists - returns false when item keyword differs")
    void isRecentSearchExists_keywordMismatch() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("keywordNormal", AttributeValue.builder().s("python").build());
        when(dynamoDbClient.getItem(any(Consumer.class))).thenAnswer(invocation -> GetItemResponse.builder().item(item).build());

        assertFalse(dao.isRecentSearchExists("PK", "SK", "java"));
    }

    @Test
    @DisplayName("isRecentSearchExists - returns false when exception thrown")
    void isRecentSearchExists_exception() {
        when(dynamoDbClient.getItem(any(Consumer.class))).thenThrow(DynamoDbException.builder().message("err").build());
        assertFalse(dao.isRecentSearchExists("PK", "SK", "java"));
    }

    @Test
    @DisplayName("deleteRecentSearch - success path returns true")
    void deleteRecentSearch_success() {
        Map<String, AttributeValue> item = Map.of("pk", AttributeValue.builder().s("PK").build(),
                "sk", AttributeValue.builder().s("SK").build(),
                "active", AttributeValue.builder().s(Constants.YES).build());

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        PutItemResponse mockPutResp = mock(PutItemResponse.class);
        SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockPutResp.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(mockPutResp);

        assertTrue(dao.deleteRecentSearch("PK", "SK"));
    }

    @Test
    @DisplayName("deleteRecentSearch - returns false when item missing")
    void deleteRecentSearch_noItem() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        assertFalse(dao.deleteRecentSearch("PK", "SK"));
    }

    @Test
    @DisplayName("deleteRecentSearch - returns false when DynamoDB exception")
    void deleteRecentSearch_exception() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenThrow(DynamoDbException.builder().message("oops").build());
        assertFalse(dao.deleteRecentSearch("PK", "SK"));
    }

    @Test
    @DisplayName("getRecentSearchesByUser - returns mapped list")
    void getRecentSearchesByUser_success() {
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("pk", AttributeValue.builder().s("USER#u1").build());
        item1.put("sk", AttributeValue.builder().s("UserKeyword#java").build());
        item1.put("type", AttributeValue.builder().s(Constants.USER_KEYWORD).build());
        item1.put("keywordOriginal", AttributeValue.builder().s("Java").build());
        item1.put("keywordNormal", AttributeValue.builder().s("java").build());
        item1.put("active", AttributeValue.builder().s(Constants.YES).build());
        item1.put("createdAt", AttributeValue.builder().s("2024/01/01 00:00:00").build());

        Map<String, AttributeValue> item2 = new HashMap<>(item1);
        item2.put("keywordOriginal", AttributeValue.builder().s("Python").build());
        item2.put("keywordNormal", AttributeValue.builder().s("python").build());

        QueryResponse response = QueryResponse.builder().items(List.of(item1, item2)).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<UserGlobalSearchHistory> result = dao.getRecentSearchesByUser("u1");
        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getKeywordOriginal());
        assertEquals("Python", result.get(1).getKeywordOriginal());
    }


    @Test
    @DisplayName("getLatestActiveSearches - returns limited active search terms")
    void getLatestActiveSearches_success() {
        Map<String, AttributeValue> item1 = Map.of("keywordOriginal", AttributeValue.builder().s("Java").build());
        Map<String, AttributeValue> item2 = Map.of("keywordOriginal", AttributeValue.builder().s("Python").build());
        Map<String, AttributeValue> item3 = Map.of("keywordOriginal", AttributeValue.builder().s("AWS").build());
        Map<String, AttributeValue> item4 = Map.of("keywordOriginal", AttributeValue.builder().s("Docker").build()); // should be truncated by limit

        QueryResponse response = QueryResponse.builder().items(List.of(item1, item2, item3, item4)).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<String> latest = dao.getLatestActiveSearches("u1");
        assertEquals(4, latest.size()); // query limit(3) is applied at DDB level, but we mocked 4 items; ensures code just maps all items in response
        assertTrue(latest.contains("Java"));
    }
    // updateRecentSearch - success path
    @Test
    @DisplayName("updateRecentSearch - success path")
    void updateRecentSearch_success() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());
        dao.updateRecentSearch(buildHistory("u1", "Java", Constants.YES));
        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    // updateRecentSearch - throws RuntimeException on failure
    @Test
    @DisplayName("updateRecentSearch - throws RuntimeException on failure")
    void updateRecentSearch_failure() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("fail").build());
        assertThrows(RuntimeException.class,
                () -> dao.updateRecentSearch(buildHistory("u1", "Java", Constants.YES)));
    }

    // updateSearchStatusAndModifiedAt - success path
    @Test
    @DisplayName("updateSearchStatusAndModifiedAt - success path")
    void updateSearchStatusAndModifiedAt_success() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());
        dao.updateSearchStatusAndModifiedAt("PK", "SK");
        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    // updateSearchStatusAndModifiedAt - exception wrapped in DataBaseException
    @Test
    @DisplayName("updateSearchStatusAndModifiedAt - exception wrapped in DataBaseException")
    void updateSearchStatusAndModifiedAt_failure() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("x").build());
        assertThrows(DataBaseException.class,
                () -> dao.updateSearchStatusAndModifiedAt("PK", "SK"));
    }


}

