package com.cognizant.lms.userservice.dao;


import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap; // ensure HashMap available for merging

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Repository
@Slf4j
public class TenantSettingsDaoImpl implements TenantSettingsDao{
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;
  private final UserService userservice;
  private final DynamoDbTable<Tenant> tenantTable;

  public TenantSettingsDaoImpl(DynamoDbClient dynamoDbClient,
                               DynamoDBConfig dynamoDBConfig,
                               @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tableName,
                               UserService userservice) {
    this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
    this.userservice = userservice;
    this.tenantTable = dynamoDBConfig.getDynamoDBEnhancedClient()
        .table(tableName, TableSchema.fromBean(Tenant.class));
  }



  @Override
  public TenantSettingsResponse getTenant(String settingName) {
    String tenantCode = TenantUtil.getTenantCode();
    TenantSettingsResponse tenantSettingsResponse = null;

    try {
      String type = Constants.SETTING;
      String sortKey = tenantCode + Constants.HASH + settingName;

      Map<String, AttributeValue> expressionValues = Map.of(
          ":type", AttributeValue.builder().s(type).build(),
          ":sk", AttributeValue.builder().s(sortKey).build()
      );

      String projectionExpression = "#type, createdOn, createdBy, settingName, reviewEmail, courseReviewCommentType, updatedBy, updatedDate";

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName(Constants.TENANT_SETTING_GSI_TYPE)
          .keyConditionExpression("#type = :type AND sk = :sk")
          .expressionAttributeNames(Map.of("#type", "type"))
          .expressionAttributeValues(expressionValues)
          .projectionExpression(projectionExpression)
          .build();

      QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
      log.info("GSI query response: {}", queryResponse);

      tenantSettingsResponse = queryResponse.items().stream()
          .map(this::mapItemToTenantSettingsResponse)
          .findFirst()
          .orElse(null);

    } catch (DynamoDbException e) {
      log.error("Error querying tenant setting '{}' using GSI: {}", settingName, e.getMessage());
      throw new DataBaseException("Error querying tenant setting " + settingName + ": " + e.getMessage());
    }

    return tenantSettingsResponse;
  }

  /**
   * Get tenant feature flags based on user's tenant code
   * @return
   */
  @Override
  public TenantFeatureFlagsDto getTenantFeatureFlags(String tenantCode) {
    try {
      Map<String, AttributeValue> exprValues = Map.of(
              ":type", AttributeValue.builder().s(Constants.TENANTTYPE).build(),
              ":tenantPk", AttributeValue.builder().s(tenantCode).build()
      );
      QueryRequest query = QueryRequest.builder().tableName(tableName)
              .indexName(Constants.TENANT_SETTING_GSI_TYPE)
              .keyConditionExpression("#type = :type")
              .expressionAttributeNames(Map.of(
              "#type", "type",
              "#pk", "pk",
              "#sk", "sk",
              "#name", "name",
              "#featureFlags", "featureFlags"
          ))
              .expressionAttributeValues(exprValues)
              .filterExpression("pk = :tenantPk")
          .projectionExpression("#pk, #sk, #name, #featureFlags")
              .build();
      QueryResponse response = dynamoDbClient.query(query);
      log.info("GSI query response for type Tenant: -> {}", response);
      if (response.items().isEmpty()) {
        return null;
      }
     return  response.items().stream()
              .map(this::mapFeatureFlags)
              .findFirst()
              .orElse(null);

    }
    catch(DynamoDbException e)
      {
        log.error("Error querying tenant feature flags using GSI: {}", e.getMessage());
        throw new DataBaseException("Error querying tenant feature flags: " + e.getMessage());
      }
    catch(Exception e)
    {
      log.error("General error querying tenant feature flags: {}", e.getMessage());
      throw new DataBaseException("General error querying tenant feature flags: " + e.getMessage());
    }
  }

  /**
   *  Map DynamoDB item to TenantFeatureFlagsDto
   * @param item
   * @return
   */
  private TenantFeatureFlagsDto mapFeatureFlags(Map<String, AttributeValue> item) {
    TenantFeatureFlagsDto dto = new TenantFeatureFlagsDto();
    dto.setPk(item.get("pk").s());
    dto.setSk(item.get("sk").s());
    dto.setName(item.get("name").s());


    if (item.containsKey("featureFlags") && item.get("featureFlags").s() != null) {
      String featureFlagsJson = item.get("featureFlags").s();
      try {

         ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Boolean> featureFlags = objectMapper.readValue(
            featureFlagsJson, new TypeReference<>() {}
        );
        dto.setFeatureFlags(featureFlags);
      } catch (Exception e) {
        log.error("Failed to parse featureFlags JSON: {}", e.getMessage());
        dto.setFeatureFlags(Collections.emptyMap()); // fallback
      }
    }
    return dto;
  }


  private TenantSettingsResponse mapItemToTenantSettingsResponse(Map<String, AttributeValue> item) {
    TenantSettingsResponse response = new TenantSettingsResponse();

    for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
      try {
        Field field = TenantSettingsResponse.class.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        AttributeValue attributeValue = entry.getValue();
        if (attributeValue != null && attributeValue.s() != null) {
          field.set(response, attributeValue.s());
        } else {
          log.warn("Null or empty value for field: {}", entry.getKey());
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.info("Error mapping tenant setting field '{}' : {}", entry.getKey(), e.getMessage());
      }
    }

    try {
      Field updatedByField = TenantSettingsResponse.class.getDeclaredField("updatedBy");
      updatedByField.setAccessible(true);
      if (updatedByField.get(response) == null) {
        updatedByField.set(response, "");
      }

      Field updatedDateField = TenantSettingsResponse.class.getDeclaredField("updatedDate");
      updatedDateField.setAccessible(true);
      if (updatedDateField.get(response) == null) {
        updatedDateField.set(response, "");
      }

    } catch (NoSuchFieldException | IllegalAccessException ex) {
      log.info("Error setting default values for updatedBy/updatedDate: {}", ex.getMessage());
    }

    return response;
  }


  @Override
  public boolean updateTenantSetings(Tenant tenant, String updatedBy, String updatedDate) {
    Map<String, AttributeValue> key = Map.of(
        "pk", AttributeValue.builder().s(tenant.getPk()).build(),
        "sk", AttributeValue.builder().s(tenant.getSk()).build()
    );

    try {
      Map<String, AttributeValue> exprValues = Map.of(
          ":reviewEmail", AttributeValue.builder().s(tenant.getReviewEmail()).build(),
          ":commentType", AttributeValue.builder().s(tenant.getCourseReviewCommentType()).build(),
          ":updatedBy", AttributeValue.builder().s(updatedBy).build(),
          ":updatedDate", AttributeValue.builder().s(updatedDate).build()
      );

      String updateExpr = "SET reviewEmail = :reviewEmail, courseReviewCommentType = :commentType, " +
          "updatedBy = :updatedBy, updatedDate = :updatedDate";

      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(key)
          .updateExpression(updateExpr)
          .expressionAttributeValues(exprValues)
          .conditionExpression("attribute_exists(pk) AND attribute_exists(sk)")
          .build();

      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      return response.sdkHttpResponse().isSuccessful();

    } catch (ConditionalCheckFailedException e) {
      log.error("No existing tenant setting found for PK: {}, SK: {}.", tenant.getPk(), tenant.getSk());
      return false;

    } catch (DynamoDbException ex) {
      log.error("Error writing to DynamoDB: {}", ex.getMessage());
      throw new DataBaseException("DynamoDB operation failed: " + ex.getMessage());
    }
  }

  @Override
  public boolean createTenantSettings(Tenant tenant) {
    try {
      tenantTable.putItem(tenant);
      return true;
    } catch (DynamoDbException ex) {
      log.error("Error creating tenant settings: {}", ex.getMessage());
      throw new DataBaseException("Failed to create tenant settings: " + ex.getMessage());
    }
  }

  /**
   * Merges provided feature flags with existing ones and updates in DynamoDB
   * @param featureFlags
   * @return
   */
  @Override
  public boolean updateTenantFeatureFlags(Map<String, Boolean> featureFlags, String tenantCode) {
    try {
      TenantFeatureFlagsDto existing = getTenantFeatureFlags(tenantCode);

      String pk = existing.getPk();
      String sk = existing.getSk();
      if (pk == null || sk == null) {
        log.error("Missing pk/sk on existing tenant feature flags item.");
        return false;
      }

      // Start with existing flags (may be null)
      Map<String, Boolean> merged = existing.getFeatureFlags() == null ? new HashMap<>() : new HashMap<>(existing.getFeatureFlags());
      if (featureFlags != null) {
        featureFlags.forEach((k,v) -> {
          if (v != null) { // only add/update non-null values
            merged.put(k, v);
          }
        });
      }

      ObjectMapper mapper = new ObjectMapper();
      String flagsJson = mapper.writeValueAsString(merged);

      Map<String, AttributeValue> key = Map.of(
          "pk", AttributeValue.builder().s(pk).build(),
          "sk", AttributeValue.builder().s(sk).build()
      );

      Map<String, AttributeValue> exprValues = Map.of(
          ":featureFlags", AttributeValue.builder().s(flagsJson).build()
      );

      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(key)
          .updateExpression("SET featureFlags = :featureFlags")
          .expressionAttributeValues(exprValues)
          .conditionExpression("attribute_exists(pk) AND attribute_exists(sk)")
          .build();

      UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
      boolean success = response.sdkHttpResponse().isSuccessful();
      log.info("Merged tenant feature flags update status={} pk={} sk={} size={}", success, pk, sk, merged.size());
      return success;
    } catch (ConditionalCheckFailedException e) {
      log.error("Conditional check failed updating feature flags: {}", e.getMessage());
      return false;
    } catch (DynamoDbException e) {
      log.error("DynamoDB error updating feature flags: {}", e.getMessage());
      throw new DataBaseException("DynamoDB error updating feature flags: " + e.getMessage());
    } catch (Exception e) {
      log.error("General error updating feature flags: {}", e.getMessage());
      throw new DataBaseException("General error updating feature flags: " + e.getMessage());
    }
  }

}
