package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.domain.UserGlobalSearchHistory;
import com.cognizant.lms.userservice.dto.RecentSearchRequest;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class UserGlobalSearchHistoryDaoImpl implements UserGlobalSearchHistoryDao {

  private final DynamoDbClient dynamoDbClient;
  private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
  private final String UserGlobalSearchHistoryTable;


  public UserGlobalSearchHistoryDaoImpl(DynamoDBConfig dynamoDBConfig,
                                        @Value("${AWS_DYNAMODB_USER_GLOBAL_SEARCH_HISTORY_TABLE_NAME}") String UserGlobalSearchHistoryTable) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    dynamoDbEnhancedClient = dynamoDBConfig.dynamoDbEnhancedClient(dynamoDbClient);
    this.UserGlobalSearchHistoryTable = UserGlobalSearchHistoryTable;
  }

  @Override
  public void saveRecentSearch(UserGlobalSearchHistory userGlobalSearchHistory) {
    try {
      Map<String, AttributeValue> itemValues = new HashMap<>();
      itemValues.put("pk", AttributeValue.builder().s(userGlobalSearchHistory.getPk()).build());
      itemValues.put("sk", AttributeValue.builder().s(userGlobalSearchHistory.getSk()).build());
      itemValues.put("type", AttributeValue.builder().s(userGlobalSearchHistory.getType()).build());
      itemValues.put("keywordOriginal", AttributeValue.builder().s(userGlobalSearchHistory.getKeywordOriginal()).build());
      itemValues.put("keywordNormal", AttributeValue.builder().s(userGlobalSearchHistory.getKeywordNormal()).build());
      itemValues.put("createdAt", AttributeValue.builder().s(userGlobalSearchHistory.getCreatedAt()).build());
      itemValues.put("active", AttributeValue.builder().s(userGlobalSearchHistory.getActive()).build());
      itemValues.put("gsiPk", AttributeValue.builder().s(userGlobalSearchHistory.getPk()).build());
      itemValues.put("gsiSk", AttributeValue.builder().s(userGlobalSearchHistory.getGsiSk()).build());
      dynamoDbClient.putItem(builder -> builder.tableName(UserGlobalSearchHistoryTable).item(itemValues));
    } catch (DynamoDbException e) {
      log.error("Error saving recent search for searchQuery: {} - {}", userGlobalSearchHistory.getKeywordOriginal(), e.getMessage());
      throw new DataBaseException("Error saving recent search: " + e.getMessage());
    }
  }

  @Override
  public boolean isRecentSearchExists(String pk, String sk, String searchQuery) {
    try {
      Map<String, AttributeValue> key = Map.of(
          "pk", AttributeValue.builder().s(pk).build(),
          "sk", AttributeValue.builder().s(sk).build()
      );
      var response = dynamoDbClient.getItem(builder -> builder
          .tableName(UserGlobalSearchHistoryTable)
          .key(key)
      );
      if (response.hasItem()) {
        String keywordNormal = response.item().get("keywordNormal").s();
        return keywordNormal.equals(searchQuery);
      }
    } catch (DynamoDbException e) {
      log.error("Error checking recent search existence for {}", e.getMessage());
    }
    return false;
  }


  @Override
  public boolean deleteRecentSearch(String pk, String sk) {
    try {
      // Step 1: Fetch existing item
      GetItemRequest getItemRequest = GetItemRequest.builder()
              .tableName(UserGlobalSearchHistoryTable)
              .key(Map.of(
                      "pk", AttributeValue.builder().s(pk).build(),
                      "sk", AttributeValue.builder().s(sk).build()
              ))
              .build();

      GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);

      if (!getItemResponse.hasItem()) {
        log.warn("No item found for pk: {}, sk: {}", pk, sk);
        return false;
      }

      // Make a mutable copy
      Map<String, AttributeValue> existingItem = new HashMap<>(getItemResponse.item());

      // Step 2: Update only required fields
      long epochMillis = System.currentTimeMillis();
      String newGsiSk = Constants.ACTIVE + Constants.HASH + Constants.NO + Constants.HASH +
              Constants.TIME_STAMP + Constants.HASH + epochMillis;

      existingItem.put("active", AttributeValue.builder().s(Constants.NO).build());
      existingItem.put("gsiSk", AttributeValue.builder().s(newGsiSk).build());

      // Step 3: Save updated item back
      PutItemRequest putItemRequest = PutItemRequest.builder()
              .tableName(UserGlobalSearchHistoryTable)
              .item(existingItem)
              .build();

      PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
      return putItemResponse.sdkHttpResponse().isSuccessful();

    } catch (DynamoDbException e) {
      log.error("Error updating recent search for pk: {}, sk: {} - {}", pk, sk, e.getMessage());
      return false;
    }
  }

  @Override
  public List<UserGlobalSearchHistory> getRecentSearchesByUser(String userId) {
    String pk = Constants.USER_PK + Constants.HASH + userId;

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":pk", AttributeValue.builder().s(pk).build());

    QueryRequest queryRequest = QueryRequest.builder()
        .tableName(UserGlobalSearchHistoryTable)
        .keyConditionExpression("pk = :pk")
        .expressionAttributeValues(expressionValues)
        .scanIndexForward(false) // true = oldest first, false = newest first
        .build();

    QueryResponse result = dynamoDbClient.query(queryRequest);
    List<UserGlobalSearchHistory> searches = new ArrayList<>();

    for (Map<String, AttributeValue> item : result.items()) {
      searches.add(mapToUserGlobalSearchHistory(item));
    }

    return searches;
  }


  @Override
  public void updateRecentSearch(UserGlobalSearchHistory updatedSearch) {

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String modifiedAt = ZonedDateTime.now(ZoneOffset.UTC).format(outputFormatter);

    try {
      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
              .tableName(UserGlobalSearchHistoryTable)
              .key(Map.of(
                      "pk", AttributeValue.builder().s(updatedSearch.getPk()).build(),
                      "sk", AttributeValue.builder().s(updatedSearch.getSk()).build()
              ))
              .updateExpression("SET #active = :active, #gsiSk = :gsiSk, #modifiedAt = :modifiedAt")
              .expressionAttributeNames(Map.of(
                      "#active", "active",
                      "#gsiSk", "gsiSk",
                      "#modifiedAt", "modifiedAt"
              ))
              .expressionAttributeValues(Map.of(
                      ":active", AttributeValue.builder().s(updatedSearch.getActive()).build(),
                      ":gsiSk", AttributeValue.builder().s(updatedSearch.getGsiSk()).build(),
                      ":modifiedAt", AttributeValue.builder().s(modifiedAt).build()
              ))
              .build();

      dynamoDbClient.updateItem(updateItemRequest);
    } catch (DynamoDbException e) {
      log.error("Error updating recent search for pk: {}, sk: {} - {}", updatedSearch.getPk(), updatedSearch.getSk(), e.getMessage());
      throw new RuntimeException("Failed to update recent search: " + e.getMessage(), e);
    }
  }
  private UserGlobalSearchHistory mapToUserGlobalSearchHistory(Map<String, AttributeValue> item) {
    UserGlobalSearchHistory history = new UserGlobalSearchHistory();
    history.setPk(item.get("pk") != null ? item.get("pk").s() : null);
    history.setSk(item.get("sk") != null ? item.get("sk").s() : null);
    history.setType(item.get("type") != null ? item.get("type").s() : null);
    history.setKeywordOriginal(item.get("keywordOriginal") != null ? item.get("keywordOriginal").s() : null);
    history.setKeywordNormal(item.get("keywordNormal") != null ? item.get("keywordNormal").s() : null);
    history.setActive(item.get("active") != null ? item.get("active").s() : null);
    history.setCreatedAt(item.get("createdAt") != null ? item.get("createdAt").s() : null);
    history.setGsiSk(String.valueOf(item.get("gsiSk") != null ? item.get("gsiSk").s() : null));
    return history;
  }

  @Override
  public List<String> getLatestActiveSearches(String userId) {
    String gsiPk = Constants.USER_PK + Constants.HASH + userId;

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":gsiPk", AttributeValue.builder().s(gsiPk).build());
    expressionValues.put(":activePrefix", AttributeValue.builder().s("ACTIVE#Y#TS#").build());

    QueryRequest queryRequest = QueryRequest.builder()
        .tableName(UserGlobalSearchHistoryTable)
        .indexName(Constants.GSI_USER_SEARCH_HISTORY) // Replace with your actual GSI name
        .keyConditionExpression("gsiPk = :gsiPk AND begins_with(gsiSk, :activePrefix)")
        .expressionAttributeValues(expressionValues)
        .scanIndexForward(false) // true = oldest first, false = newest first
        .limit(3)
        .build();

    QueryResponse result = dynamoDbClient.query(queryRequest);
    List<String> searches = new ArrayList<>();

    for (Map<String, AttributeValue> item : result.items()) {
      String keyword = item.getOrDefault("keywordOriginal", AttributeValue.builder().s("").build()).s();
      searches.add(keyword);

    }

    return searches;
  }



  @Override
  public void updateSearchStatusAndModifiedAt(String pk, String sk) {
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String modifiedAt = ZonedDateTime.now(ZoneOffset.UTC).format(outputFormatter);

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("pk", AttributeValue.builder().s(pk).build());
    key.put("sk", AttributeValue.builder().s(sk).build());

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":active", AttributeValue.builder().s(Constants.YES).build());
    expressionValues.put(":modifiedAt", AttributeValue.builder().s(modifiedAt).build());

    UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(UserGlobalSearchHistoryTable)
        .key(key)
        .updateExpression("SET active = :active, modifiedAt = :modifiedAt")
        .expressionAttributeValues(expressionValues)
        .build();
    try {
      log.info("Updating search status and modifiedAt for pk: {}, sk: {}", pk, sk);
      dynamoDbClient.updateItem(updateRequest);
    } catch (DynamoDbException e) {
      log.error("Error logging update operation for pk: {}, sk: {} - {}", pk, sk, e.getMessage());
      throw new DataBaseException("Error updating search status and modifiedAt: " + e.getMessage());

    }
  }
}

