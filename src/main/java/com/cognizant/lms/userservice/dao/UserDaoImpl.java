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

  @Override
  public void updateUserAspirations(String pk, String sk, String selectedUserRole, String selectedInterests) {
    try {
      log.info("Updating aspirations for user with pk: {}", pk);
      
      StringBuilder updateExpression = new StringBuilder("SET ");
      Map<String, AttributeValue> attributeValues = new HashMap<>();
      boolean isFirstField = true;

      if (selectedUserRole != null) {
        updateExpression.append("selectedUserRole = :selectedUserRole");
        attributeValues.put(":selectedUserRole", AttributeValue.builder().s(selectedUserRole).build());
        isFirstField = false;
      }

      if (selectedInterests != null) {
        if (!isFirstField) updateExpression.append(", ");
        updateExpression.append("selectedInterests = :selectedInterests");
        attributeValues.put(":selectedInterests", AttributeValue.builder().s(selectedInterests).build());
      }

      if (attributeValues.isEmpty()) {
        log.info("No aspirations to update for user with pk: {}", pk);
        return;
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

      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      log.info("User aspirations updated successfully: {}", response);
    } catch (DynamoDbException e) {
      log.error("Error updating user aspirations: {}", e.getMessage());
      throw new RuntimeException("Failed to update user aspirations in DynamoDB", e);
    }
  }

  @Override
  public void updateUserPersonalDetails(String pk, String sk, String firstName, String lastName, String country, String institutionName, String currentRole) {
    try {
      log.info("Updating personal details for user with pk: {}", pk);
      
      StringBuilder updateExpression = new StringBuilder("SET ");
      Map<String, AttributeValue> attributeValues = new HashMap<>();
      boolean isFirstField = true;

      if (firstName != null && !firstName.trim().isEmpty()) {
        updateExpression.append("firstName = :firstName");
        attributeValues.put(":firstName", AttributeValue.builder().s(firstName.trim()).build());
        isFirstField = false;
      }

      if (lastName != null && !lastName.trim().isEmpty()) {
        if (!isFirstField) updateExpression.append(", ");
        updateExpression.append("lastName = :lastName");
        attributeValues.put(":lastName", AttributeValue.builder().s(lastName.trim()).build());
        isFirstField = false;
      }

      if (country != null && !country.trim().isEmpty()) {
        if (!isFirstField) updateExpression.append(", ");
        updateExpression.append("country = :country");
        attributeValues.put(":country", AttributeValue.builder().s(country.trim()).build());
        isFirstField = false;
      }

      if (institutionName != null && !institutionName.trim().isEmpty()) {
        if (!isFirstField) updateExpression.append(", ");
        updateExpression.append("institutionName = :institutionName");
        attributeValues.put(":institutionName", AttributeValue.builder().s(institutionName.trim()).build());
        isFirstField = false;
      }

      if (currentRole != null && !currentRole.trim().isEmpty()) {
        if (!isFirstField) updateExpression.append(", ");
        updateExpression.append("currentRole = :currentRole");
        attributeValues.put(":currentRole", AttributeValue.builder().s(currentRole.trim()).build());
        isFirstField = false;
      }

      if (attributeValues.isEmpty()) {
        log.info("No personal details to update for user with pk: {}", pk);
        return;
      }

      // Add modifiedOn timestamp
      if (!isFirstField) updateExpression.append(", ");
      updateExpression.append("modifiedOn = :modifiedOn");
      attributeValues.put(":modifiedOn", AttributeValue.builder().s(String.valueOf(System.currentTimeMillis())).build());

      // Build the update request
      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(userTable.tableName())
          .key(Map.of(
              "pk", AttributeValue.builder().s(pk).build(),
              "sk", AttributeValue.builder().s(sk).build()
          ))
          .updateExpression(updateExpression.toString())
          .expressionAttributeValues(attributeValues)
          .build();

      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      log.info("User personal details updated successfully: {}", response);
    } catch (DynamoDbException e) {
      log.error("Error updating user personal details: {}", e.getMessage());
      throw new RuntimeException("Failed to update user personal details in DynamoDB", e);
    }
  }

  @Override
  public void updateProfilePhotoUrl(String pk, String sk, String profilePhotoUrl) {
    try {
      log.info("Updating profile photo URL for user with pk: {}", pk);

      Map<String, AttributeValue> attributeValues = new HashMap<>();
      attributeValues.put(":modifiedOn", AttributeValue.builder().s(String.valueOf(System.currentTimeMillis())).build());

      String updateExpression;
      if (profilePhotoUrl == null || profilePhotoUrl.trim().isEmpty()) {
        // Remove the attribute if null/empty
        updateExpression = "REMOVE profilePhotoUrl SET modifiedOn = :modifiedOn";
      } else {
        // Set the attribute value
        attributeValues.put(":profilePhotoUrl", AttributeValue.builder().s(profilePhotoUrl).build());
        updateExpression = "SET profilePhotoUrl = :profilePhotoUrl, modifiedOn = :modifiedOn";
      }

      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(userTable.tableName())
          .key(Map.of(
              "pk", AttributeValue.builder().s(pk).build(),
              "sk", AttributeValue.builder().s(sk).build()
          ))
          .updateExpression(updateExpression)
          .expressionAttributeValues(attributeValues)
          .build();

      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      log.info("Profile photo URL updated successfully: {}", response);
    } catch (DynamoDbException e) {
      log.error("Error updating profile photo URL: {}", e.getMessage());
      throw new RuntimeException("Failed to update profile photo URL in DynamoDB", e);
    }
  }
}
