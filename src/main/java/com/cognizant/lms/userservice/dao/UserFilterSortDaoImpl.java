package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.UserEmailDto;
import com.cognizant.lms.userservice.dto.UserListResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.exception.UserFilterSortException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import java.lang.reflect.Field;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cognizant.lms.userservice.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@Repository
@Slf4j
public class UserFilterSortDaoImpl implements UserFilterSortDao {

  private DynamoDbClient dynamoDbClient;
  private String tableName;
  private String partitionKeyName;


  public UserFilterSortDaoImpl(DynamoDBConfig dynamoDBConfig,
                               @Value("${AWS_DYNAMODB_USER_TABLE_NAME}") String tableName,
                               @Value("${AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME}")
                               String partitionKeyName) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
    this.partitionKeyName = partitionKeyName;
  }

  private static List<String> getList(AttributeValue attributeValue) {
    return attributeValue != null
        ? attributeValue.l().stream().map(AttributeValue::s).collect(Collectors.toList()) :
        List.of();
  }



  @Override
  public UserListResponse getUsers(String sortKey, String order,
                                   Map<String, String> lastEvaluatedKey,
                                   int perPage, String userRole, String institutionName,
                                   String searchValue, String status) {

    String partitionKeyValue =TenantUtil.getTenantCode();
    UserListResponse response = new UserListResponse();
    List<User> itemsToReturn = new ArrayList<>();
    int currentPageSize = 0;
    int limit = perPage;
    Map<String, AttributeValue> lastEvaluatedKeyMap =
        convertStringMapToAttributeValueMap(lastEvaluatedKey);

    try {

      // Prepare filter expressions and attribute values
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());
      String filterExpression = null;
      Map<String, String> expressionAttributeNames = new HashMap<>();

      if (userRole != null && institutionName != null) {
        filterExpression = getRoleAndInstitutionFilterExpression(
            userRole, institutionName, searchValue);
        expressionAttributeNames.putAll(
            getRoleAndInstitutionExpressionAttributeNames(userRole, institutionName, searchValue));
        expressionAttributeValues.putAll(
            getRoleAndInstitutionExpressionAttributeValues(userRole, institutionName,
                partitionKeyValue, searchValue));
      } else if (userRole != null) {
        filterExpression = getRoleFilterExpression(userRole, searchValue);
        expressionAttributeNames.putAll(getRoleExpressionAttributeNames(userRole, searchValue));
        expressionAttributeValues.putAll(
            getRoleExpressionAttributeValues(userRole, partitionKeyValue, searchValue));
      } else if (institutionName != null) {
        filterExpression = getInstitutionFilterExpression(institutionName, searchValue);
        expressionAttributeNames.putAll(
            getInstitutionExpressionAttributeNames(institutionName, searchValue));
        expressionAttributeValues.putAll(
            getInstitutionExpressionAttributeValues(
                institutionName, partitionKeyValue, searchValue));
      } else if (searchValue != null) {
        if (searchValue.contains("@")) {
          filterExpression = "contains(#emailId, :emailId)";
          expressionAttributeNames.put("#emailId", "emailId");
          expressionAttributeValues.put(
              ":emailId", AttributeValue.builder().s(searchValue).build());
        } else {
          filterExpression = "contains(#name, :name)";
          expressionAttributeNames.put("#name", "name");
          expressionAttributeValues.put(
              ":name", AttributeValue.builder().s(searchValue).build());
        }
      }

      if (status != null) {
        if (filterExpression != null) {
          filterExpression += " AND #status = :status";
        } else {
          filterExpression = "#status = :status";
        }
        expressionAttributeNames.put("#status", "status");
        expressionAttributeValues.put(":status", AttributeValue.builder().s(status).build());
      }

      // Get total count using do-while loop for accurate pagination count
      QueryRequest.Builder countQueryRequestBuilder = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_" + sortKey)
          .keyConditionExpression(partitionKeyName + " = :PK")
          .expressionAttributeValues(expressionAttributeValues)
          .select("COUNT");

      if (filterExpression != null) {
        countQueryRequestBuilder.filterExpression(filterExpression)
            .expressionAttributeNames(expressionAttributeNames);
      }

      log.info("Getting total count with sortKey: {}", sortKey);
      int count = 0;
      Map<String, AttributeValue> countLastEvaluatedKey = null;

      do {
        QueryRequest countQueryRequest = countQueryRequestBuilder.build();
        if (countLastEvaluatedKey != null) {
          countQueryRequest = countQueryRequest.toBuilder()
              .exclusiveStartKey(countLastEvaluatedKey)
              .build();
        }

        QueryResponse countQueryResult = dynamoDbClient.query(countQueryRequest);
        count += countQueryResult.count();
        countLastEvaluatedKey = countQueryResult.lastEvaluatedKey();
      } while (countLastEvaluatedKey != null && !countLastEvaluatedKey.isEmpty());

      log.info("Total count of records: {}", count);

      do {
        // Query to get the paginated results
        QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
            .tableName(tableName)
            .indexName("gsi_sort_" + sortKey)
            .scanIndexForward(!order.equalsIgnoreCase("desc"))
            .keyConditionExpression(partitionKeyName + " = :PK")
            .expressionAttributeValues(expressionAttributeValues)
            .limit(limit);

        if (lastEvaluatedKeyMap != null) {
          queryRequestBuilder.exclusiveStartKey(lastEvaluatedKeyMap);
        }

        if (filterExpression != null) {
          queryRequestBuilder.filterExpression(filterExpression)
              .expressionAttributeNames(expressionAttributeNames);
        }

        QueryRequest queryRequest = queryRequestBuilder.build();
        QueryResponse queryResult = dynamoDbClient.query(queryRequest);

        List<User> users = queryResult.items().stream()
            .map(this::mapItemToUser)
            .toList();

        itemsToReturn.addAll(users);
        currentPageSize += queryResult.count();
        lastEvaluatedKeyMap = queryResult.lastEvaluatedKey();

          if (currentPageSize < perPage && !lastEvaluatedKeyMap.isEmpty()) {
              limit = perPage - currentPageSize;
        } else {
          break;
        }
      } while (currentPageSize < perPage && !lastEvaluatedKeyMap.isEmpty());
      response.setUserList(itemsToReturn);
      response.setLastEvaluatedKey(lastEvaluatedKeyMap);
      response.setCount(count);
    } catch (DynamoDbException e) {
      log.error("Error reading from DynamoDB {} : ", e.getMessage());
      throw new UserFilterSortException(
          "Error reading from Users table in DynamoDB " + e.getMessage());
    }
    return response;
  }

  String getRoleAndInstitutionFilterExpression(String userRoles, String institutionNames,
                                               String searchValue) {


    List<String> roles = List.of(userRoles.split(","));
    List<String> institutions = List.of(institutionNames.split(","));
    StringBuilder filterExpression = new StringBuilder();

    if (!userRoles.contains(",") && !institutionNames.contains(",")) {
      filterExpression.append(
          "contains(#role, :role) AND contains(#institutionName, :institutionName)");
    } else {
      // Handle roles
      for (int i = 0; i < roles.size(); i++) {
        if (i > 0) {
          filterExpression.append(" AND ");
        }
        filterExpression.append("contains(#role").append(i + 1).append(", :role").append(i + 1)
            .append(")");
      }

      // Handle institutions
      if (!institutions.isEmpty()) {
        if (!roles.isEmpty()) {
          filterExpression.append(" AND ");
        }
        filterExpression.append("(");
        for (int i = 0; i < institutions.size(); i++) {
          if (i > 0) {
            filterExpression.append(" OR ");
          }
          filterExpression.append("contains(#institutionName").append(i + 1)
              .append(", :institutionName").append(i + 1).append(")");
        }
        filterExpression.append(")");
      }
    }
    // Handle searchValue
    if (searchValue != null) {
      if (!filterExpression.isEmpty()) {
        filterExpression.append(" AND ");
      }
      if (searchValue.contains("@")) {
        filterExpression.append("contains(#emailId, :emailId)");
      } else {
        filterExpression.append("contains(#name, :name)");
      }
    }

    return filterExpression.toString();
  }

  Map<String, String> getRoleAndInstitutionExpressionAttributeNames(
          String userRoles, String institutionNames,
          String searchValue) {
    Map<String, String> expressionAttributeNames = new HashMap<>();

    if (!userRoles.contains(",") && !institutionNames.contains(",")) {
      expressionAttributeNames.put("#role", "role");
      expressionAttributeNames.put("#institutionName", "institutionName");
    } else {
      List<String> roles = List.of(userRoles.split(","));
      for (int i = 0; i < roles.size(); i++) {
        expressionAttributeNames.put("#role" + (i + 1), "role");
      }

      List<String> institutions = List.of(institutionNames.split(","));
      for (int i = 0; i < institutions.size(); i++) {
        expressionAttributeNames.put("#institutionName" + (i + 1), "institutionName");
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeNames.put("#emailId", "emailId");
      } else {
        expressionAttributeNames.put("#name", "name");
      }
    }

    return expressionAttributeNames;
  }

  Map<String, AttributeValue> getRoleAndInstitutionExpressionAttributeValues(
          String userRoles, String institutionNames,
          String partitionKeyValue, String searchValue) {
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());

    if (!userRoles.contains(",") && !institutionNames.contains(",")) {
      expressionAttributeValues.put(":role", AttributeValue.builder().s(userRoles).build());
      expressionAttributeValues.put(
          ":institutionName", AttributeValue.builder().s(institutionNames).build());
    } else {
      List<String> roles = List.of(userRoles.split(","));
      for (int i = 0; i < roles.size(); i++) {
        expressionAttributeValues.put(
            ":role" + (i + 1), AttributeValue.builder().s(roles.get(i)).build());
      }

      List<String> institutions = List.of(institutionNames.split(","));
      for (int i = 0; i < institutions.size(); i++) {
        expressionAttributeValues.put(":institutionName" + (i + 1),
            AttributeValue.builder().s(institutions.get(i)).build());
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeValues.put(":emailId", AttributeValue.builder().s(searchValue).build());
      } else {
        expressionAttributeValues.put(
            ":name", AttributeValue.builder().s(searchValue).build());
      }
    }

    return expressionAttributeValues;
  }


  String getRoleFilterExpression(String userRole, String searchValue) {
    StringBuilder filterExpression = new StringBuilder();

    if (!userRole.contains(",")) {
      filterExpression.append("contains(#role, :role)");
    } else {
      List<String> roles = List.of(userRole.split(","));
      for (int i = 0; i < roles.size(); i++) {
        if (i > 0) {
          filterExpression.append(" AND ");
        }
        filterExpression
            .append("contains(#role")
            .append(i + 1)
            .append(", :role")
            .append(i + 1)
            .append(")");
      }
    }

    if (searchValue != null) {
      if (!filterExpression.isEmpty()) {
        filterExpression.append(" AND ");
      }
      if (searchValue.contains("@")) {
        filterExpression.append("contains(#emailId, :emailId)");
      } else {
        filterExpression.append("contains(#name, :name)");
      }
    }
    return filterExpression.toString();
  }

  Map<String, String> getRoleExpressionAttributeNames(String userRoles,
                                                      String searchValue) {
    Map<String, String> expressionAttributeNames = new HashMap<>();

    if (!userRoles.contains(",")) {
      expressionAttributeNames.put("#role", "role");
    } else {
      List<String> roles = List.of(userRoles.split(","));
      for (int i = 0; i < roles.size(); i++) {
        expressionAttributeNames.put("#role" + (i + 1), "role");
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeNames.put("#emailId", "emailId");
      } else {
        expressionAttributeNames.put("#name", "name");
      }
    }

    return expressionAttributeNames;
  }

  Map<String, AttributeValue> getRoleExpressionAttributeValues(
          String userRoles, String partitionKeyValue,
          String searchValue) {
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());

    if (!userRoles.contains(",")) {
      expressionAttributeValues.put(":role", AttributeValue.builder().s(userRoles).build());
    } else {
      List<String> roles = List.of(userRoles.split(","));
      for (int i = 0; i < roles.size(); i++) {
        expressionAttributeValues.put(
            ":role" + (i + 1), AttributeValue.builder().s(roles.get(i)).build());
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeValues.put(":emailId", AttributeValue.builder().s(searchValue).build());
      } else {
        expressionAttributeValues.put(
            ":name", AttributeValue.builder().s(searchValue).build());
      }
    }
    return expressionAttributeValues;
  }

  String getInstitutionFilterExpression(String institutionName, String searchValue) {
    StringBuilder filterExpression = new StringBuilder();

    // Handle searchValue first
    if (searchValue != null) {
      if (searchValue.contains("@")) {
        filterExpression.append("contains(#emailId, :emailId)");
      } else {
        filterExpression.append("contains(#name, :name)");
      }
    }

    // Handle institutionName
    if (!institutionName.contains(",")) {
      if (!filterExpression.isEmpty()) {
        filterExpression.append(" AND ");
      }
      filterExpression.append("contains(#institutionName, :institutionName)");
    } else {
      List<String> institutionNames = List.of(institutionName.split(","));
      if (!filterExpression.isEmpty()) {
        filterExpression.append(" AND ");
      }
      filterExpression.append("(");
      for (int i = 0; i < institutionNames.size(); i++) {
        if (i > 0) {
          filterExpression.append(" OR ");
        }
        filterExpression.append("contains(#institutionName").append(i + 1)
            .append(", :institutionName").append(i + 1).append(")");
      }
      filterExpression.append(")");
    }
    return filterExpression.toString();
  }

  Map<String, String> getInstitutionExpressionAttributeNames(
          String institutionName, String searchValue) {
    Map<String, String> expressionAttributeNames = new HashMap<>();

    if (!institutionName.contains(",")) {
      expressionAttributeNames.put("#institutionName", "institutionName");
    } else {
      List<String> institutionNames = List.of(institutionName.split(","));
      for (int i = 0; i < institutionNames.size(); i++) {
        expressionAttributeNames.put("#institutionName" + (i + 1), "institutionName");
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeNames.put("#emailId", "emailId");
      } else {
        expressionAttributeNames.put("#name", "name");
      }
    }

    return expressionAttributeNames;
  }

  Map<String, AttributeValue> getInstitutionExpressionAttributeValues(
          String institutionName, String partitionKeyValue, String searchValue) {
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());

    if (!institutionName.contains(",")) {
      expressionAttributeValues.put(
          ":institutionName", AttributeValue.builder().s(institutionName).build());
    } else {
      List<String> institutionNames = List.of(institutionName.split(","));
      for (int i = 0; i < institutionNames.size(); i++) {
        expressionAttributeValues.put(
            ":institutionName" + (i + 1),
            AttributeValue.builder().s(institutionNames.get(i)).build());
      }
    }

    if (searchValue != null) {
      if (searchValue.contains("@")) {
        expressionAttributeValues.put(":emailId", AttributeValue.builder().s(searchValue).build());
      } else {
        expressionAttributeValues.put(
            ":name", AttributeValue.builder().s(searchValue).build());
      }
    }
    return expressionAttributeValues;
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

  private User mapItemToUser(Map<String, AttributeValue> item) {
    User user = new User();
    for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {

      try {
        Field field = User.class.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        AttributeValue attributeValue = entry.getValue();
        if (attributeValue != null && attributeValue.s() != null) {
          field.set(user, attributeValue.s());
        } else {
          log.warn("Null value found for field: {}", entry.getKey());
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.info("Error occurred while mapping user attributes {} : ", e.getMessage());
      }
    }
    return user;
  }

  @Override
  public List<String> getInstitutions(String sortKey) {
    List<String> institutions = new ArrayList<>();
    String partitionKeyValue = TenantUtil.getTenantCode();
    try {
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_" + sortKey)
          .keyConditionExpression(partitionKeyName + " = :PK")
          .projectionExpression("institutionName")
          .scanIndexForward(true)
          .expressionAttributeValues(
              Map.of(
                  ":PK", AttributeValue.builder().s(partitionKeyValue).build()
              ))
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);

      institutions = queryResult.items().stream()
          .map(item -> item.get("institutionName").s())
          .distinct()
          .collect(Collectors.toList());
      log.info("Institutions from GSI: {}", institutions);
    } catch (DynamoDbException e) {
      log.error("Error reading roles from DynamoDB {} : ", e.getMessage());
    }
    return institutions;
  }

  @Override
  public User getUserByEmailId(String emailId, String status) {
    User user;
    try {
      emailId = emailId.toLowerCase();
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_emailId")
          .keyConditionExpression("emailId = :emailId")
          .filterExpression("#status = :status AND #tenantCode = :tenantCode")
          .expressionAttributeNames(Map.of("#status", "status", "#tenantCode", "tenantCode"))
          .expressionAttributeValues(
              Map.of(
                  ":emailId", AttributeValue.builder().s(emailId).build(),
                  ":status", AttributeValue.builder().s(status).build(),
                  ":tenantCode", AttributeValue.builder().s(TenantUtil.getTenantCode()).build()
              ))
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      user = queryResult.items().stream()
          .map(this::mapItemToUser).findFirst().orElse(null);
    } catch (DynamoDbException e) {
      log.error("Error reading from DynamoDB {} : ", e.getMessage());
      throw new DataBaseException("Error reading from DynamoDB");
    }
    return user;
  }

  @Override
  public boolean deactivateUser(User user, String modifiedBy) {
    try {
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String updatedOn = utcDateTime.format(formatter);
      String userExpiryDate = updatedOn.split(" ")[0];
      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression(
              "SET #status = :status, #modifiedOn = :modifiedOn, #modifiedBy = :modifiedBy,"
                  + " #role = :role, #userAccountExpiryDate = :userAccountExpiryDate")
          .expressionAttributeNames(Map.of("#status", "status",
              "#modifiedOn", "modifiedOn", "#modifiedBy", "modifiedBy",
              "#role", "role", "#userAccountExpiryDate", "userAccountExpiryDate"))
          .expressionAttributeValues(
              Map.of(
                  ":status", AttributeValue.builder().s(Constants.IN_ACTIVE_STATUS).build(),
                  ":modifiedOn", AttributeValue.builder().s(updatedOn).build(),
                  ":modifiedBy", AttributeValue.builder().s(modifiedBy).build(),
                  ":role", AttributeValue.builder().s(Constants.ROLE_LEARNER).build(),
                  ":userAccountExpiryDate", AttributeValue.builder().s(userExpiryDate).build()
              ))
          .build();
      UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
      return updateItemResponse.sdkHttpResponse().isSuccessful();
    } catch (DynamoDbException e) {
      log.error("Error updating user status in DynamoDB {} : ", e.getMessage());
      throw new DataBaseException("Error updating user status in DynamoDB " + e.getMessage());
    }
  }
  @Override
  public boolean reActivateUser(User user, String newExpiryDate) {
    try {
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String updatedOn = utcDateTime.format(formatter);
      DateTimeFormatter formatterForLastlogTimeStamp = DateTimeFormatter.ofPattern(Constants.TIMESTAMP);
      String lastLoginTimestamp = utcDateTime.format(formatterForLastlogTimeStamp);
      String newStatus = Constants.ACTIVE_STATUS;
      String newRole = Constants.ROLE_LEARNER;
      String modifiedBy = UserContext.getModifiedBy();
      user.setReactivatedDate(lastLoginTimestamp);
      String reactivatedDate = user.getReactivatedDate();

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression(
              "SET #status = :status, #role = :role, #userAccountExpiryDate = :userAccountExpiryDate, " +
                  "#modifiedOn = :modifiedOn, #modifiedBy = :modifiedBy, #reactivatedDate = :reactivatedDate, " +
                  "#lastLoginTimestamp = :lastLoginTimestamp"
          )
          .expressionAttributeNames(Map.of(
              "#status", "status",
              "#role", "role",
              "#userAccountExpiryDate", "userAccountExpiryDate",
              "#modifiedOn", "modifiedOn",
              "#modifiedBy", "modifiedBy",
              "#reactivatedDate", "reactivatedDate",
              "#lastLoginTimestamp", "lastLoginTimestamp"
          ))
          .expressionAttributeValues(Map.of(
              ":status", AttributeValue.builder().s(newStatus).build(),
              ":role", AttributeValue.builder().s(newRole).build(),
              ":userAccountExpiryDate", AttributeValue.builder().s(newExpiryDate).build(),
              ":modifiedOn", AttributeValue.builder().s(updatedOn).build(),
              ":modifiedBy", AttributeValue.builder().s(modifiedBy).build(),
              ":reactivatedDate", AttributeValue.builder().s(reactivatedDate).build(),
              ":lastLoginTimestamp", AttributeValue.builder().s(lastLoginTimestamp).build()
          ))
          .build();

      UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
      return updateItemResponse.sdkHttpResponse().isSuccessful();
    } catch (DynamoDbException e) {
      log.error("Error activating user in DynamoDB {} : ", e.getMessage());
      throw new DataBaseException("Error activating user in DynamoDB " + e.getMessage());
    }
  }

  @Override
  public boolean updateUser(User user, String modifiedBy) {
    try {
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String updatedOn = utcDateTime.format(formatter);
      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression(
              "SET #firstName = :firstName, #lastName = :lastName, "
                  + "#institutionName = :institutionName, "
                  + "#userAccountExpiryDate = :userAccountExpiryDate, "
                  + "#modifiedOn = :modifiedOn, #modifiedBy = :modifiedBy, "
                  + "#gsiSortFNLN = :gsiSortFNLN, #name = :name, "
                  + "#country = :country" //Added country field
          )
          .expressionAttributeNames(Map.of(
              "#firstName", "firstName",
              "#lastName", "lastName",
              "#institutionName", "institutionName",
              "#userAccountExpiryDate", "userAccountExpiryDate",
              "#modifiedOn", "modifiedOn",
              "#modifiedBy", "modifiedBy",
              "#gsiSortFNLN", "gsiSortFNLN",
              "#name", "name",
              "#country", "country" //Added country field
          ))
          .expressionAttributeValues(Map.of(
              ":firstName", AttributeValue.builder().s(user.getFirstName()).build(),
              ":lastName", AttributeValue.builder().s(user.getLastName()).build(),
              ":institutionName", AttributeValue.builder().s(user.getInstitutionName()).build(),
              ":userAccountExpiryDate",
              AttributeValue.builder().s(user.getUserAccountExpiryDate()).build(),
              ":modifiedOn", AttributeValue.builder().s(updatedOn).build(),
              ":modifiedBy", AttributeValue.builder().s(modifiedBy).build(),
              ":gsiSortFNLN", AttributeValue.builder().s(user.getGsiSortFNLN()).build(),
              ":name", AttributeValue.builder().s(user.getName()).build(),
              ":country", AttributeValue.builder().s(user.getCountry()).build() //Added country field
          ))
          .build();

      UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
      return updateItemResponse.sdkHttpResponse().isSuccessful();
    } catch (DynamoDbException e) {
      log.error("Error updating user in DynamoDB {} : ", e.getMessage());
      throw new DataBaseException("Error updating user in DynamoDB");
    }
  }

  @Override
  public User getUserByPk(String partitionKeyValue) {
    User user = null;
    try {
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression(Constants.PARTITION_KEY_NAME + " = :PK")
          .expressionAttributeValues(
              Map.of(
                  ":PK", AttributeValue.builder().s(partitionKeyValue).build()
              )
          )
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      if (!queryResult.items().isEmpty()) {
        user = queryResult.items().stream()
            .map(this::mapItemToUser).findFirst().orElse(null);
      }
    } catch (DynamoDbException e) {
      log.error("Error fetching user with partition key {} from DynamoDB: {}",
          partitionKeyValue, e.getMessage());
      throw new DataBaseException("Error fetching user with partition key "
          + partitionKeyValue + "from DynamoDB" + e.getMessage());
    }
    return user;
  }

  @Override
  public boolean updateUserByPk(User user, String modifiedBy) {
    try {
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String updatedOn = utcDateTime.format(formatter);
      DateTimeFormatter formatterForLastLoginTimeStamp = DateTimeFormatter.ofPattern(Constants.TIMESTAMP);
      String lastLoginTime = utcDateTime.format(formatterForLastLoginTimeStamp);

      Map<String, String> expressionAttributeNames = getExpressionNamesForUpdateUsersByPk();
      Map<String, AttributeValue> expressionAttributeValues = getExpressionValuesForUpdateUsersByPk(user, updatedOn, modifiedBy);

      StringBuilder updateExpression = new StringBuilder("SET #firstName = :firstName, #modifiedOn = :modifiedOn, #modifiedBy = :modifiedBy,"
          + " #lastName = :lastName, #institutionName = :institutionName, #role = :role,"
          + " #userAccountExpiryDate = :userAccountExpiryDate, #userType = :userType,"
          + " #status = :status, #gsiSortFNLN = :gsiSortFNLN, #name = :name, #country = :country");

      if (user.getReactivatedDate() != null) {
        updateExpression.append(", #reactivatedDate = :reactivatedDate, #lastLoginTimestamp = :lastLoginTimestamp");

        expressionAttributeNames.put("#reactivatedDate", "reactivatedDate");
        expressionAttributeNames.put("#lastLoginTimestamp", "lastLoginTimestamp");

        expressionAttributeValues.put(":reactivatedDate", AttributeValue.builder().s(user.getReactivatedDate()).build());
        expressionAttributeValues.put(":lastLoginTimestamp", AttributeValue.builder().s(lastLoginTime).build());
      }

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression(updateExpression.toString())
          .expressionAttributeNames(expressionAttributeNames)
          .expressionAttributeValues(expressionAttributeValues)
          .build();

      UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
      return updateItemResponse.sdkHttpResponse().isSuccessful();
    } catch (DynamoDbException e) {
      log.error("Error updating the user ByPk {} in DynamoDB {} : ", user.getPk(), e.getMessage());
      throw new DataBaseException("Error updating the user ByPk " + user.getPk() + " in DynamoDB: " + e.getMessage());
    }
  }



  Map<String, String> getExpressionNamesForUpdateUsersByPk() {
    Map<String, String> expressionAttributesNames = new HashMap<>();
    expressionAttributesNames.put("#firstName", "firstName");
    expressionAttributesNames.put("#modifiedOn", "modifiedOn");
    expressionAttributesNames.put("#modifiedBy", "modifiedBy");
    expressionAttributesNames.put("#lastName", "lastName");
    expressionAttributesNames.put("#institutionName", "institutionName");
    expressionAttributesNames.put("#role", "role");
    expressionAttributesNames.put("#userAccountExpiryDate", "userAccountExpiryDate");
    expressionAttributesNames.put("#userType", "userType");
    expressionAttributesNames.put("#status", "status");
    expressionAttributesNames.put("#gsiSortFNLN", "gsiSortFNLN");
    expressionAttributesNames.put("#name", "name");
    expressionAttributesNames.put("#country", "country"); // Added country field
    return expressionAttributesNames;
  }

  Map<String, AttributeValue> getExpressionValuesForUpdateUsersByPk(User user,
                                                                    String updatedOn,
                                                                    String modifiedBy) {
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":firstName", AttributeValue.builder().s(user.getFirstName())
        .build());
    expressionAttributeValues.put(":modifiedOn", AttributeValue.builder().s(updatedOn).build());
    expressionAttributeValues.put(":modifiedBy", AttributeValue.builder().s(modifiedBy).build());
    expressionAttributeValues.put(":lastName", AttributeValue.builder().s(user.getLastName())
        .build());
    expressionAttributeValues.put(":institutionName", AttributeValue.builder()
        .s(user.getInstitutionName()).build());
    expressionAttributeValues.put(":role", AttributeValue.builder().s(user.getRole()).build());
    expressionAttributeValues.put(":userAccountExpiryDate",
        AttributeValue.builder().s(user.getUserAccountExpiryDate()).build());
    expressionAttributeValues.put(":userType", AttributeValue.builder().s(user.getUserType())
        .build());
    expressionAttributeValues.put(":status",
        AttributeValue.builder().s(user.getStatus()).build());
    expressionAttributeValues.put(":gsiSortFNLN",
        AttributeValue.builder().s(user.getGsiSortFNLN()).build());
    expressionAttributeValues.put(":name", AttributeValue.builder().s(user.getName()).build());
    expressionAttributeValues.put(":country", AttributeValue.builder().s(user.getCountry()).build()); //Added country field
    return expressionAttributeValues;
  }

  public Set<String> getExpiredUsers() {
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.FIRST_AS_YEAR_FORMAT);
    String currentDate = utcDateTime.format(formatter);
    String partitionKeyValue = TenantUtil.getTenantCode();

    try {
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName(Constants.DEFAULT_GSI)
          .keyConditionExpression(partitionKeyName + " = :PK")
          .filterExpression("#userAccountExpiryDate <= :currentDate AND #status = :status"
              + " AND #userAccountExpiryDate <> :ignoreDate")
          .expressionAttributeNames(
              Map.of("#userAccountExpiryDate", "userAccountExpiryDate",
                  "#status", "status"))
          .expressionAttributeValues(
              Map.of(
                  ":PK", AttributeValue.builder().s(partitionKeyValue).build(),
                  ":currentDate", AttributeValue.builder().s(currentDate).build(),
                  ":status", AttributeValue.builder().s(Constants.ACTIVE_STATUS).build(
                  ),
                  ":ignoreDate", AttributeValue.builder().s(Constants.IGNORE_DATE).build()
              ))
          .projectionExpression("emailId")
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      return queryResult.items().stream()
          .map(item -> item.get("emailId").s())
          .collect(Collectors.toSet());
    } catch (DynamoDbException e) {
      log.error("Error fetching expired users from DynamoDB: {}", e.getMessage());
      throw new DataBaseException("Error fetching expired users from DynamoDB");
    }
  }

  @Override
  public boolean updateFirstLoggedInUser(User user, String modifiedBy) {
    try {
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String updatedOn = utcDateTime.format(formatter);
      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression(
              "SET #lastLoginTimestamp = :lastLoginTimestamp, "
                  + "#modifiedOn = :modifiedOn, #modifiedBy = :modifiedBy"
          )
          .expressionAttributeNames(Map.of(
              "#lastLoginTimestamp", "lastLoginTimestamp",
              "#modifiedOn", "modifiedOn",
              "#modifiedBy", "modifiedBy"
          ))
          .expressionAttributeValues(Map.of(
              ":lastLoginTimestamp", AttributeValue.builder().s(utcDateTime.toString()).build(),
              ":modifiedOn", AttributeValue.builder().s(updatedOn).build(),
              ":modifiedBy", AttributeValue.builder().s(modifiedBy).build()
          ))
          .build();
      UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
      return updateItemResponse.sdkHttpResponse().isSuccessful();
    } catch (DynamoDbException e) {
      log.error("Error updating first time loggedIn user in DynamoDB {} : ", e.getMessage());
      throw new DataBaseException(
          "Error updating first time loggedIn user in DynamoDB " + e.getMessage());
    }
  }

  @Override
  public boolean updateUserTermsAccepted(String pk, String sk, String termsAccepted, String termsAcceptedDate) {
      try {
          Map<String, String> expressionAttributeNames = new HashMap<>();
          Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
          String updateExpression = "SET #termsAccepted = :termsAccepted";

          expressionAttributeNames.put("#termsAccepted", "termsAccepted");
          expressionAttributeValues.put(":termsAccepted", AttributeValue.builder().s(termsAccepted).build());

          if (termsAcceptedDate != null) {
              updateExpression += ", #termsAcceptedDate = :termsAcceptedDate";
              expressionAttributeNames.put("#termsAcceptedDate", "termsAcceptedDate");
              expressionAttributeValues.put(":termsAcceptedDate", AttributeValue.builder().s(termsAcceptedDate).build());
          }

          UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                  .tableName(tableName)
                  .key(Map.of(
                          "pk", AttributeValue.builder().s(pk).build(),
                          "sk", AttributeValue.builder().s(sk).build()
                  ))
                  .updateExpression(updateExpression)
                  .expressionAttributeNames(expressionAttributeNames)
                  .expressionAttributeValues(expressionAttributeValues)
                  .build();

          UpdateItemResponse updateResponse = dynamoDbClient.updateItem(updateItemRequest);
          if (!updateResponse.sdkHttpResponse().isSuccessful()) {
              log.error("Failed to update termsAccepted for user with pk: {}, sk: {}", pk, sk);
              return false;
          }
          return true;
      } catch (DynamoDbException e) {
          log.error("Error updating termsAccepted for user with pk: {}, sk: {} - {}", pk, sk, e.getMessage());
          return false;
      }
  }

  @Override
  public void deleteBatchUsers(List<User> users) {
    int batchSize = 25;
    try {
      for (int i = 0; i < users.size(); i += batchSize) {
        List<User> batch = users
            .subList(i, Math.min(i + batchSize, users.size()));
        List<WriteRequest> deleteRequests = new ArrayList<>();
        for (User userItem : batch) {
          deleteRequests.add(WriteRequest.builder()
              .deleteRequest(DeleteRequest.builder()
                  .key(Map.of(
                      "pk", AttributeValue.builder().s(userItem.getPk()).build(),
                      "sk", AttributeValue.builder().s(userItem.getSk()).build()
                  ))
                  .build())
              .build());
        }

        BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
            .requestItems(Map.of(tableName, deleteRequests))
            .build();

        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        if (response.sdkHttpResponse().isSuccessful()) {
          log.info("Successfully deleted the list of items from {} table", tableName);
        } else {
          log.error("Unexpected error: Failed to delete the list of Rows in {} table", tableName);
          throw new DataBaseException("Unexpected error: Failed to delete the list of Rows");
        }
      }
    } catch (DynamoDbException exception) {
      log.error("Failed to delete the list of Rows in {} table: {}", tableName,
          exception.awsErrorDetails().errorMessage());
      throw new DataBaseException("Failed to delete the list of Rows: "
          + exception.awsErrorDetails().errorMessage());
    }
  }

  @Override
  public void updateUserSettings(String userId, String type, String option) {
    try {
      String sortKeyValue = TenantUtil.getTenantCode() + Constants.HASH + Constants.USER_SETTINGS;

      // 1. Define placeholders for the Expression Attribute Names and Values
      String updateExpressionName = "#" + type;
      String updateExpressionValue = ":" + type;

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(userId).build(),
              "sk", AttributeValue.builder().s(sortKeyValue).build()
          ))
          // 2. Use the dynamic placeholders in the SET expression
          .updateExpression("SET " + updateExpressionName + " = " + updateExpressionValue)
          .expressionAttributeNames(Map.of(
              updateExpressionName, type // Maps #voiceId -> "voiceId"
          ))
          .expressionAttributeValues(Map.of(
              updateExpressionValue, AttributeValue.builder().s(option).build() // Maps :voiceId -> "value"
          ))
          .build();

      dynamoDbClient.updateItem(updateItemRequest);
      log.info("Successfully updated {} for userId: {}", type, userId);
    } catch (DynamoDbException e) {
      log.error("Error updating user settings ({}) for userId {}: {}", type, userId, e.getMessage());
      throw new DataBaseException("Error updating user settings in DynamoDB for userId " + userId);
    }
  }


  @Override
  public boolean updateTermsAccepted() {
    try {
      String partitionKeyValue = TenantUtil.getTenantCode();

      // First, query all users with userType = "External"
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName(Constants.DEFAULT_GSI)
          .keyConditionExpression(partitionKeyName + " = :PK")
          .filterExpression("#userType = :userType")
          .expressionAttributeNames(Map.of("#userType", "userType"))
          .expressionAttributeValues(Map.of(
              ":PK", AttributeValue.builder().s(partitionKeyValue).build(),
              ":userType", AttributeValue.builder().s("External").build()
          ))
          .build();

      QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

      // Update each external user's termsAccepted to true
      boolean allUpdatesSuccessful = true;
      for (Map<String, AttributeValue> item : queryResponse.items()) {
        String pk = item.get("pk").s();
        String sk = item.get("sk").s();

        boolean updateResult = updateUserTermsAccepted(pk, sk, "N", null);
        if (!updateResult) {
          allUpdatesSuccessful = false;
          log.error("Failed to update termsAccepted for user with pk: {}, sk: {}", pk, sk);
        }

      }

      log.info("Updated termsAccepted for {} external users", queryResponse.items().size());
      return allUpdatesSuccessful;

    } catch (DynamoDbException e) {
      log.error("Error querying external users from DynamoDB: {}", e.getMessage());
      return false;
    }
  }

  public Map<String, String> getUserEmailsByUsernames(List<String> fnlnList) {
    String tenantCode = TenantUtil.getTenantCode();
    Map<String, String> emailMap = new HashMap<>();

    log.info("Starting user email lookup for {} FNLN values under tenantCode '{}'", fnlnList.size(), tenantCode);

    for (String fnlnKey : fnlnList) {
      String normalizedFNLN = fnlnKey.trim(); // preserve original casing
      log.info("Normalized input '{}' to '{}'", fnlnKey, normalizedFNLN);

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_name")
          .keyConditionExpression("#tenantCode = :tenantCode AND #gsiSortFNLN = :gsiSortFNLN")
          .expressionAttributeNames(Map.of(
              "#tenantCode", "tenantCode",
              "#gsiSortFNLN", "gsiSortFNLN"
          ))
          .expressionAttributeValues(Map.of(
              ":tenantCode", AttributeValue.builder().s(tenantCode).build(),
              ":gsiSortFNLN", AttributeValue.builder().s(normalizedFNLN).build()
          ))
          .build();


      try {
        log.debug("Executing query for gsiSortFNLN '{}' under tenantCode '{}'", normalizedFNLN, tenantCode);
        QueryResponse queryResult = dynamoDbClient.query(queryRequest);
        List<Map<String, AttributeValue>> items = queryResult.items();

        if (items != null && !items.isEmpty()) {
          Map<String, AttributeValue> firstItem = items.get(0);
          String email = firstItem.get("emailId") != null ? firstItem.get("emailId").s() : null;

          if (email != null) {
            log.info("Found email '{}' for FNLN '{}'", email, fnlnKey);
          } else {
            log.warn("Email field is missing or null for FNLN '{}'", fnlnKey);
          }

          emailMap.put(fnlnKey, email);
        } else {
          log.info("No user found in DynamoDB for FNLN '{}'", fnlnKey);
        }
      } catch (DynamoDbException e) {
        log.info("DynamoDB query failed for FNLN '{}': {}", fnlnKey, e.getMessage(), e);
      } catch (Exception e) {
        log.info("Unexpected error while processing FNLN '{}': {}", fnlnKey, e.getMessage(), e);
      }
    }


    log.info("Completed user email lookup. Successfully resolved {} entries. Failed: {}",
        emailMap.size(), fnlnList.size() - emailMap.size());

    return emailMap;
  }

  @Override
  public boolean updateLearnerPreferredView(User user, String preferredUI) {
    try {
      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(Map.of(
              "pk", AttributeValue.builder().s(user.getPk()).build(),
              "sk", AttributeValue.builder().s(user.getSk()).build()
          ))
          .updateExpression("SET #preferredUI = :preferredUI")
          .expressionAttributeNames(Map.of("#preferredUI", "preferredUI"))
          .expressionAttributeValues(Map.of(":preferredUI",
              AttributeValue.builder().s(preferredUI).build()))
          .build();

      UpdateItemResponse updateResponse = dynamoDbClient.updateItem(updateItemRequest);
      if (!updateResponse.sdkHttpResponse().isSuccessful()) {
        log.error("Failed to update preferredUI for user with pk: {}", user.getPk());
        throw new DataBaseException("Failed to update preferredUI for user with pk: " + user.getPk());
      }
      return true;
    } catch (DynamoDbException e) {
      log.error("Error updating preferredUI for user with pk: {} - {}", user.getPk(), e.getMessage());
      throw new DataBaseException("Error updating preferredUI for user with pk: " + user.getPk() + " - " + e.getMessage());
    }
  }

  @Override
  public List<UserEmailDto> listUserEmailIdAndUserId() {
    List<UserEmailDto> emails = new ArrayList<>();
    String tenantCode = TenantUtil.getTenantCode();
    try {
      // Use GSI where tenantCode is HASH and status is RANGE (gsi_sort_status)
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_status")
          .keyConditionExpression("tenantCode = :tenantCode AND #status = :status")
          .expressionAttributeNames(Map.of("#status", "status"))
          .expressionAttributeValues(Map.of(
              ":tenantCode", AttributeValue.builder().s(tenantCode).build(),
              ":status", AttributeValue.builder().s("Active").build()
          ))
          .projectionExpression("emailId, pk")
          .build();
      QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
      for (Map<String, AttributeValue> item : queryResponse.items()) {
        AttributeValue emailAttr = item.get("emailId");
        AttributeValue userIdAttr = item.get("pk");
        if (emailAttr != null && emailAttr.s() != null && userIdAttr != null && userIdAttr.s() != null) {
          emails.add(new UserEmailDto(emailAttr.s(), userIdAttr.s()));
        }
      }
    } catch (Exception e) {
      log.error("Error fetching user emails: {}", e.getMessage());
      throw new RuntimeException("Failed to fetch user emails", e);
    }
    return emails;
  }

  @Override
  public boolean updateIsWatchedTutorial(String pk, String sk) {
    // Always set isWatchedTutorial to "Y"
    String isWatchedTutorial = "Y";
    try {

      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
      String tutorialWatchDate = utcDateTime.format(formatter);

      Map<String, String> expressionAttributeNames = new HashMap<>();
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      String updateExpression = "SET #isWatchedTutorial = :isWatchedTutorial";

      expressionAttributeNames.put("#isWatchedTutorial", "isWatchedTutorial");
      expressionAttributeValues.put(":isWatchedTutorial", AttributeValue.builder().s(isWatchedTutorial).build());

      if (tutorialWatchDate != null) {
        updateExpression += ", #tutorialWatchDate = :tutorialWatchDate";
        expressionAttributeNames.put("#tutorialWatchDate", "tutorialWatchDate");
        expressionAttributeValues.put(":tutorialWatchDate", AttributeValue.builder().s(tutorialWatchDate).build());
      }

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
              .tableName(tableName)
              .key(Map.of(
                      "pk", AttributeValue.builder().s(pk).build(),
                      "sk", AttributeValue.builder().s(sk).build()
              ))
              .updateExpression(updateExpression)
              .expressionAttributeNames(expressionAttributeNames)
              .expressionAttributeValues(expressionAttributeValues)
              .build();

      UpdateItemResponse updateResponse = dynamoDbClient.updateItem(updateItemRequest);
      if (!updateResponse.sdkHttpResponse().isSuccessful()) {
        log.error("Failed to update isWatchedTutorial for user with pk: {}, sk: {}", pk, sk);
        return false;
      }
      return true;
    } catch (DynamoDbException e) {
      log.error("Error updating isWatchedTutorial for user with pk: {}, sk: {} - {}", pk, sk, e.getMessage());
      return false;
    }
  }

  @Override
  public boolean updateVideoLaunchCount(String pk, String sk) {
    try {
      Map<String, String> expressionAttributeNames = new HashMap<>();
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      String updateExpression = "ADD #videoLaunchCount :increment";

      expressionAttributeNames.put("#videoLaunchCount", "videoLaunchCount");
      expressionAttributeValues.put(":increment", AttributeValue.builder().n("1").build());

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
              .tableName(tableName)
              .key(Map.of(
                      "pk", AttributeValue.builder().s(pk).build(),
                      "sk", AttributeValue.builder().s(sk).build()
              ))
              .updateExpression(updateExpression)
              .expressionAttributeNames(expressionAttributeNames)
              .expressionAttributeValues(expressionAttributeValues)
              .build();

      UpdateItemResponse updateResponse = dynamoDbClient.updateItem(updateItemRequest);
      if (!updateResponse.sdkHttpResponse().isSuccessful()) {
        log.error("Failed to increment videoLaunchCount for user with pk: {}, sk: {}", pk, sk);
        return false;
      }
      return true;
    } catch (DynamoDbException e) {
      log.error("Error incrementing videoLaunchCount for user with pk: {}, sk: {} - {}", pk, sk, e.getMessage());
      return false;
    }
  }

    /** * Retrieves a list of users by tenant code, excluding those with SSO login option.
     *
     * @param partitionKeyValue the tenant code to filter users by
     * @return a list of users associated with the specified tenant code
     */
    @Override
    public List<User> getUserByTenantCode(String partitionKeyValue) {
        List<User> users = new ArrayList<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeNames.put("#status", "status");
        expressionAttributeValues.put(":status", AttributeValue.builder().s(Constants.ACTIVE_STATUS).build());
        expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("gsi_sort_createdOn")
                    .keyConditionExpression(partitionKeyName + " = :PK")
                    .filterExpression("#status = :status")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
            QueryResponse queryResult = dynamoDbClient.query(queryRequest);
            users = queryResult.items().stream()
                    .map(this::mapItemToUser).collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error reading from DynamoDB {} : ", e.getMessage());
            throw new DataBaseException("Error reading from DynamoDB");
        }
        log.info("users fetched for tenant code {} : {}", partitionKeyValue, users);
        return users;
    }
  @Override
  public User getUserByEmailIdAndTenant(String emailId, String status, String tenantCode){
    User user;
    try {
        emailId = emailId.toLowerCase();
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("gsi_sort_emailId")
                .keyConditionExpression("emailId = :emailId")
                .filterExpression("#status = :status AND #tenantCode = :tenantCode")
                .expressionAttributeNames(Map.of("#status", "status", "#tenantCode", "tenantCode"))
                .expressionAttributeValues(
                        Map.of(
                                ":emailId", AttributeValue.builder().s(emailId).build(),
                                ":status", AttributeValue.builder().s(status).build(),
                                ":tenantCode", AttributeValue.builder().s(tenantCode).build()
                        ))
                .build();
        QueryResponse queryResult = dynamoDbClient.query(queryRequest);
        user = queryResult.items().stream()
                .map(this::mapItemToUser).findFirst().orElse(null);
    } catch (DynamoDbException e) {
        log.error("Error reading from DynamoDB {} : ", e.getMessage());
        throw new DataBaseException("Error reading from DynamoDB");
    }
    return user;
  }
}
