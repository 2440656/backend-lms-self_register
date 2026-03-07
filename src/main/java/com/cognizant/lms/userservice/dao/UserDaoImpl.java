package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UpdateDateDTO;
import com.cognizant.lms.userservice.exception.DataBaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class UserDaoImpl implements UserDao {
  private final DynamoDbTable<User> userTable;
  private final String tenantTableName;
  private final DynamoDbClient dynamoDbClient;

  @Autowired
  public UserDaoImpl(DynamoDBConfig dynamoDBConfig,
                     @Value("${AWS_DYNAMODB_USER_TABLE_NAME}") String tableName,
                     @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tenantTableName
  ) {
    this.tenantTableName = tenantTableName;
    this.userTable = dynamoDBConfig.getDynamoDBEnhancedClient()
        .table(tableName, TableSchema.fromBean(User.class));
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
  }


  @Override
  public void createUser(User user) {
    userTable.putItem(user);
  }

  //TODO need to use query instead of scan
  @Override
  public boolean userExists(String emailId) {
    return userTable.scan().items().stream().anyMatch(user -> user.getEmailId().equals(emailId));
  }

  @Override
  public TenantDTO getTenantDetails(String tenantIdentifier) {
    TenantDTO tenant;
    try {
      String projectionExpression = "pk, tenantIdentifier, #name, idpPreferences";
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tenantTableName)
          .indexName(Constants.DEFAULT_GSI)
          .keyConditionExpression("tenantIdentifier" + " = :PK")
          .filterExpression("#status = :status")
          .expressionAttributeNames(Map.of("#status", "status", "#name", "name"))
          .expressionAttributeValues(
              Map.of(
                  ":PK", AttributeValue.builder().s(tenantIdentifier).build(),
                  ":status", AttributeValue.builder().s("Active").build()
              ))
          .projectionExpression(projectionExpression)
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      tenant = queryResult.items().stream()
          .map(this::mapItemToTenant).findFirst().orElse(null);
    } catch (DynamoDbException e) {
      log.error("Error reading from DynamoDB {} : ", e.getMessage());
      throw new DataBaseException("Error reading from DynamoDB");
    }
    return tenant;
  }

  private TenantDTO mapItemToTenant(Map<String, AttributeValue> item) {
    TenantDTO tenant = new TenantDTO();
    for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {

      try {
        Field field = TenantDTO.class.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        AttributeValue attributeValue = entry.getValue();
        if (attributeValue != null && attributeValue.s() != null) {
          field.set(tenant, attributeValue.s());
        } else {
          log.warn("Null value found for field: {}", entry.getKey());
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.info("Error occurred while mapping user attributes {} : ", e.getMessage());
      }
    }
    return tenant;
  }


  @Override
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    try {
      userTable.scan().items().forEach(users::add);
    } catch (DynamoDbException e) {
      log.error("Error fetching all users: {}", e.getMessage());
      throw new RuntimeException("Failed to fetch users from DynamoDB", e);
    }
    return users;
  }


  @Override
  public void updateLastLoginTimeStampAndPasswordChangedDate(String pk, String sk, UpdateDateDTO updateDateDTO) {
    try {
      log.info("Updating user with emailId: {}", updateDateDTO.getEmailId());
      boolean isFirstField = true;
      StringBuilder updateExpression = new StringBuilder("SET ");
      Map<String, AttributeValue> attributeValues = new HashMap<>();

      // Add optional fields if they are not null
      if (updateDateDTO.getTermsAccepted() != null) {
        if (!isFirstField) {
          updateExpression.append(", ");
        }
        updateExpression.append(" termsAccepted = :termsAccepted");
        isFirstField = false;
        attributeValues.put(":termsAccepted", AttributeValue.builder().s(updateDateDTO.getTermsAccepted()).build());
      }

      if (updateDateDTO.getTermsAcceptedDate() != null) {
        if (!isFirstField) {
          updateExpression.append(", ");
        }
        updateExpression.append(" termsAcceptedDate = :termsAcceptedDate");
        isFirstField = false;
        attributeValues.put(":termsAcceptedDate", AttributeValue.builder().s(updateDateDTO.getTermsAcceptedDate()).build());
      }

      if( updateDateDTO.getPasswordChangedDate() != null){
        if (!isFirstField) {
          updateExpression.append(", ");
        }
        updateExpression.append(" passwordChangedDate = :passwordChangedDate");
        isFirstField = false;
        attributeValues.put(":passwordChangedDate", AttributeValue.builder().s(updateDateDTO.getPasswordChangedDate()).build());
      }

      if (updateDateDTO.getLastLoginTimestamp() != null) {
        if (!isFirstField) {
          updateExpression.append(", ");
        }
        updateExpression.append(" lastLoginTimestamp = :lastLoginTimestamp");
        isFirstField = false;
        attributeValues.put(":lastLoginTimestamp", AttributeValue.builder().s(updateDateDTO.getLastLoginTimestamp()).build());
      }
      if (attributeValues.isEmpty()) {
          log.info("No fields to update for user with emailId: {}", updateDateDTO.getEmailId());
          return; // Nothing to update
      }
      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(userTable.tableName())
          .key(Map.of(
              "pk", AttributeValue.builder().s(pk).build(),
              "sk", AttributeValue.builder().s(sk).build()
          ))
          .updateExpression(updateExpression.toString())
          .expressionAttributeValues(attributeValues)
          .build();

      // Execute the update
      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      log.info("User updated successfully: {}", response);
    } catch (DynamoDbException e) {
      log.error("Error updating user: {}", e.getMessage());
      throw new RuntimeException("Failed to update user in DynamoDB", e);
    }
  }

  @Override
  public void addPasswordChangedDate(String pk, String sk, String emailId, String passwordChangedDate) {
    try {
      log.info("Adding passwordChangedDate for user with emailId: {}", emailId);


      // Construct the update request
      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(userTable.tableName())
          .key(Map.of(
              "pk", AttributeValue.builder().s(pk).build(),
              "sk", AttributeValue.builder().s(sk).build()
          ))
          .updateExpression("SET passwordChangedDate = :passwordChanged")
          .expressionAttributeValues(Map.of(
              ":passwordChanged", AttributeValue.builder().s(passwordChangedDate).build()
          ))
          .build();

      // Execute the update
      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      log.info("PasswordChangedDate added successfully: {}", response);
    } catch (DynamoDbException e) {
      log.error("Error adding passwordChangedDate: {}", e.getMessage());
      throw new RuntimeException("Failed to add passwordChangedDate in DynamoDB", e);
    }
  }
}
