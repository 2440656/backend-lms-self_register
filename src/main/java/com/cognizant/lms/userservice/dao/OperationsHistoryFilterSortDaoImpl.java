package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.dto.LogFileResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@Repository
@Slf4j
public class OperationsHistoryFilterSortDaoImpl implements OperationsHistoryFilterSortDao {

  private DynamoDbClient dynamoDbClient;
  private String tableName;
  private String partitionKeyName;

  public OperationsHistoryFilterSortDaoImpl(DynamoDBConfig dynamoDBConfig,
                                            @Value("${AWS_DYNAMODB_LOGFILE_TABLE_NAME}")
                                            String tableName,
                                            @Value("${AWS_DYNAMODB_LOGFILE"
                                                + "_TABLE_PARTITION_KEY_NAME}")
                                            String partitionKeyName) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
    this.partitionKeyName = partitionKeyName;
  }

  @Override
  public LogFileResponse getLogFileLists(String partitionKeyValue, String sortKey, String order,
                                         String process, String userEmail,
                                         Map<String, String> lastEvaluatedKey,
                                         int perPage) {
    perPage = perPage < 0 || perPage > 100 ? 10 : perPage;


    LogFileResponse response = new LogFileResponse();
    List<OperationsHistory> operationsHistoryList = new ArrayList<>();
    Map<String, AttributeValue> lastEvaluatedKeyMap =
        convertStringMapToAttributeValueMap(lastEvaluatedKey);

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":pk", AttributeValue.builder().s(partitionKeyValue).build());
    StringBuilder filterExpression = new StringBuilder();
    Optional.ofNullable(process).ifPresent(processValue -> {
      filterExpression.append("contains(operation, :operation)");
      expressionAttributeValues.put(":operation",
          AttributeValue.builder().s(processValue).build());
    });
    Optional.ofNullable(userEmail).ifPresent(userEmailValue -> {
      if (!filterExpression.isEmpty()) {
        filterExpression.append(" AND ");
      }
      filterExpression.append("email = :email");
      expressionAttributeValues.put(":email",
          AttributeValue.builder().s(userEmailValue).build());
    });

    int currentPageSize = 0;
    int limit = perPage;
    try {
      do {
        QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("pk = :" + partitionKeyName)
            .consistentRead(false)
            .scanIndexForward(order.equalsIgnoreCase("asc"))
            .expressionAttributeValues(expressionAttributeValues)
            .limit(limit);

        if (lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty()) {
          queryRequestBuilder.exclusiveStartKey(lastEvaluatedKeyMap);
        }
        if (!filterExpression.isEmpty()) {
          queryRequestBuilder.filterExpression(filterExpression.toString());
        }

        QueryRequest queryRequest = queryRequestBuilder.build();
        QueryResponse queryResult = dynamoDbClient.query(queryRequest);

        List<OperationsHistory> fetchedItems = queryResult.items().stream()
            .map(this::mapItemToLogFile)
            .distinct()
            .toList();

        operationsHistoryList.addAll(fetchedItems);

        currentPageSize += queryResult.count();
        lastEvaluatedKeyMap = queryResult.lastEvaluatedKey();

        if (currentPageSize < perPage && lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty()) {
          limit = perPage - currentPageSize;
        } else {
          break;
        }
      } while (currentPageSize < perPage && lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty());

      response.setLogFiles(operationsHistoryList);
      if (!operationsHistoryList.isEmpty()) {
        response.setLastEvaluatedKey(lastEvaluatedKeyMap);
      }
      log.info("Log files from list: {}", operationsHistoryList.size());
    } catch (DynamoDbException e) {
      log.error("Error reading log files from DynamoDB: {}", e.getMessage());
      throw new DataBaseException("Error reading log files from DynamoDB ");
    }
    return response;
  }

  private OperationsHistory mapItemToLogFile(Map<String, AttributeValue> item) {
    OperationsHistory operationsHistory = new OperationsHistory();
    for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
      try {
        Field field = OperationsHistory.class.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        field.set(operationsHistory, entry.getValue() != null ? entry.getValue().s() : null);

      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.info("Error occurred while mapping ErrorLogs attributes {} : ", e.getMessage());
      }
    }
    return operationsHistory;
  }

  private Map<String, AttributeValue> convertStringMapToAttributeValueMap(
      Map<String, String> stringMap) {
    if (stringMap == null) {
      return null;
    }
    Map<String, AttributeValue> attributeValueMap = new HashMap<>();
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      attributeValueMap.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
    }
    return attributeValueMap;
  }

}
