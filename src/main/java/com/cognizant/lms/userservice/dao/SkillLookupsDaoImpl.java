package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.SkillCategoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;

@Slf4j
@Repository
public class SkillLookupsDaoImpl implements SkillLookupsDao {
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;
  private final ObjectMapper objectMapper = new ObjectMapper();


  public SkillLookupsDaoImpl(DynamoDBConfig dynamoDBConfig,
                             @Value("${AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME}") String tableName) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
  }

  private Map<String, AttributeValue> convertStringMapToAttributeValueMap(Map<String, String> stringMap) {
    Map<String, AttributeValue> attributeValueMap = new HashMap<>();
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      attributeValueMap.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
    }
    return attributeValueMap;
  }

  private SkillLookups mapToSkillLookups(Map<String, AttributeValue> item) {
    Map<String, String> itemMap = item.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              AttributeValue value = entry.getValue();
              // Handle both string and numeric types explicitly
              if (value.s() != null) {
                return value.s();
              } else if (value.n() != null) {
                return value.n(); // Convert numeric value to String
              } else {
                return null; // Handle other types if necessary
              }
            }
        ));

    return objectMapper.convertValue(itemMap, SkillLookups.class);
  }


  @Override
  public List<SkillCategoryResponse> getSkillCategory(String skillNames) {
    List<SkillCategoryResponse> skillCategoryResponses = new ArrayList<>();
    try {
      String[] skillNameArray = skillNames.split(",");
      for (String skillName : skillNameArray) {
        skillName = skillName.trim();
        // Fetch the PK of the skill
        Map<String, String> expressionAttributeNames = Map.of(
            "#type", "type",
            "#name", "name"
        );

        Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":type", AttributeValue.builder().s("Skill").build(),
            ":name", AttributeValue.builder().s(skillName).build()
        );

        QueryRequest queryRequest = QueryRequest.builder()
            .tableName(tableName)
            .indexName("gsi-Type")
            .keyConditionExpression("#type = :type and #name = :name")
            .expressionAttributeNames(expressionAttributeNames)
            .expressionAttributeValues(expressionAttributeValues)
            .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (response.items().isEmpty()) {
          continue;
        }

        String pk = response.items().get(0).get("pk").s();
        String skValue = pk.substring(pk.indexOf("#") + 1);

        expressionAttributeNames = Map.of(
            "#type", "type",
            "#sk", "sk"
        );

        expressionAttributeValues = Map.of(
            ":type", AttributeValue.builder().s("Skill-Cat").build(),
            ":sk", AttributeValue.builder().s(skValue).build()
        );

        queryRequest = QueryRequest.builder()
            .tableName(tableName)
            .indexName("gsi-Type")
            .keyConditionExpression("#type = :type")
            .filterExpression("#sk = :sk")
            .expressionAttributeNames(expressionAttributeNames)
            .expressionAttributeValues(expressionAttributeValues)
            .build();

        response = dynamoDbClient.query(queryRequest);
        if (!response.items().isEmpty()) {
          SkillCategoryResponse skillCategoryResponse = new SkillCategoryResponse();
          skillCategoryResponse.setSkillName(skillName);
          skillCategoryResponse.setSkillCategoryName(response.items().get(0).get("name").s());
          skillCategoryResponses.add(skillCategoryResponse);
        }
      }
    } catch (DynamoDbException e) {
      log.error("Error reading skill category from DynamoDB: {}", e.awsErrorDetails().errorMessage());
      throw new DataBaseException("Error reading skill category from DynamoDB: " + e.awsErrorDetails().errorMessage());
    }
    return skillCategoryResponses;
  }

  @Override
  public SkillLookupResponse getSkillsAndLookupsByNameOrCode(String type, String search) {
    List<String> typeList = Arrays.stream(type.split(","))
        .map(String::trim)
        .filter(t -> !t.isEmpty())
        .toList();

    try {
      List<SkillLookups> skills = new ArrayList<>();

      for (String typeValue : typeList) {
        boolean isSkillType = typeValue.equalsIgnoreCase(Constants.SKILL);
        String pk = TenantUtil.getTenantCode() + Constants.HASH + typeValue.toLowerCase().trim();
        List<SkillLookups> typeSkills = new ArrayList<>();

        if (isSkillType) {
          if (search == null || search.isBlank()) {
            continue;
          }
          log.info("Performing concurrent queries for skill name and code with search value: {}", search);
          String normalizedSearch = normalizeSearchValue(search);
          CompletableFuture<List<SkillLookups>> nameFuture = CompletableFuture.supplyAsync(
              () -> querySkillIndex(Constants.GSI_SKILL_NAME, "normalizedName", pk, normalizedSearch, 20)
          );
          CompletableFuture<List<SkillLookups>> codeFuture = CompletableFuture.supplyAsync(
              () -> querySkillIndex(Constants.GSI_SKILL_CODE, "normalizedCode", pk, normalizedSearch, 20)
          );

          List<SkillLookups> nameResults;
          List<SkillLookups> codeResults;
          try {
            CompletableFuture.allOf(nameFuture, codeFuture).join();
            nameResults = nameFuture.join();
            codeResults = codeFuture.join();
            log.info("Concurrent queries completed. Name results: {}, Code results: {}", nameResults.size(), codeResults.size());
          } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Error querying skill indexes concurrently: {}", cause.getMessage(), cause);
            throw new DataBaseException("Error reading skills/lookup from DynamoDB: " + cause.getMessage());
          } catch (Exception e) {
            log.error("Unexpected error querying skill indexes concurrently: {}", e.getMessage(), e);
            throw new DataBaseException("Error reading skills/lookup from DynamoDB: " + e.getMessage());
          }

          Map<String, SkillLookups> combined = new LinkedHashMap<>();
          for (SkillLookups skill : nameResults) {
            combined.put(buildSkillKey(skill), skill);
          }
          for (SkillLookups skill : codeResults) {
            combined.put(buildSkillKey(skill), skill);
          }
          typeSkills.addAll(combined.values());
        } else {
          typeSkills.addAll(queryAllByTenantType(pk));
        }

        if (isSkillType) {
            typeSkills = sortSkills(typeSkills);
        } else if (typeValue.equalsIgnoreCase(Constants.SKILL_PROF_LEVEL)) {
          typeSkills = sortSkillProficiencyLevels(typeSkills);
        }

        skills.addAll(typeSkills);
      }

      if (skills.isEmpty()) {
        log.info("No skills found for type(s) '{}' with search '{}'", type, search);
        return new SkillLookupResponse(null, null, 204, "No skills found");
      }
      log.info("Fetched {} skills for type(s) '{}' with search '{}'", skills.size(), type, search);
      return new SkillLookupResponse(skills, null, 200, null);
    } catch (DynamoDbException e) {
      log.error("Error reading skills/lookup from DynamoDB: {}", e.awsErrorDetails().errorMessage());
      throw new DataBaseException("Error reading skills/lookup from DynamoDB: " + e.awsErrorDetails().errorMessage());
    }
  }

  private List<SkillLookups> sortSkills(List<SkillLookups> typeSkills) {
    return typeSkills != null ? typeSkills.stream()
        .sorted(Comparator.comparing(
            skill -> skill.getNormalizedName() != null ? skill.getNormalizedName() : "",
            Comparator.nullsLast(String::compareTo)
        ))
        .toList() : new ArrayList<>();
  }

  private List<SkillLookups> sortSkillProficiencyLevels(List<SkillLookups> typeSkills) {
    return typeSkills != null ? typeSkills.stream()
        .sorted(Comparator.comparing(
            skill -> {
              String sk = skill.getSk();
              try {
                return Integer.parseInt(sk.replaceAll("[^\\d]", ""));
              } catch (Exception e) {
                return Integer.MAX_VALUE;
              }
            },
            Comparator.nullsLast(Integer::compareTo)
        ))
        .toList() : new ArrayList<>();
  }

  private List<SkillLookups> querySkillIndex(String indexName,
                                             String sortKeyAttribute,
                                             String pk,
                                             String search,
                                             int limit) {
    if (search == null || search.isBlank()) {
      return new ArrayList<>();
    }

    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#pk", "tenantType");
    expressionAttributeNames.put("#sk", sortKeyAttribute);

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":pk", AttributeValue.builder().s(pk).build());
    expressionAttributeValues.put(":search", AttributeValue.builder().s(search).build());

    QueryRequest queryRequest = QueryRequest.builder()
        .tableName(tableName)
        .indexName(indexName)
        .keyConditionExpression("#pk = :pk and begins_with(#sk, :search)")
        .expressionAttributeNames(expressionAttributeNames)
        .expressionAttributeValues(expressionAttributeValues)
        .limit(limit)
        .build();

    QueryResponse response = dynamoDbClient.query(queryRequest);
    return response.items().stream()
        .map(this::mapToSkillLookups)
        .toList();
  }

  private List<SkillLookups> queryAllByTenantType(String pk) {
    List<SkillLookups> results = new ArrayList<>();
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":pk", AttributeValue.builder().s(pk).build());

    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#pk", "tenantType");

    Map<String, AttributeValue> lastEvaluatedKey = null;
    do {
      QueryRequest.Builder queryBuilder = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_skillName")
          .keyConditionExpression("#pk = :pk")
          .expressionAttributeNames(expressionAttributeNames)
          .expressionAttributeValues(expressionAttributeValues);

      if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
        queryBuilder.exclusiveStartKey(lastEvaluatedKey);
      }

      QueryResponse response = dynamoDbClient.query(queryBuilder.build());
      for (Map<String, AttributeValue> item : response.items()) {
        results.add(this.mapToSkillLookups(item));
      }
      lastEvaluatedKey = response.lastEvaluatedKey();
    } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

    return results;
  }

  private String normalizeSearchValue(String value) {
    if (value == null) {
      return null;
    }
    return value.toLowerCase().replaceAll("\\s+", "");
  }

  private String buildSkillKey(SkillLookups skill) {
    String pk = skill != null ? skill.getPk() : "";
    String sk = skill != null ? skill.getSk() : "";
    return pk + "|" + sk;
  }

  @Override
  public void uploadSkills(List<SkillLookups> skills) {
      final int BATCH_SIZE = 25;
      List<Map<String, AttributeValue>> batch = new ArrayList<>();
      int totalBatches = 0;
      int totalRecords = 0;

      for (SkillLookups skill : skills) {
        String tenantType = TenantUtil.getTenantCode() + Constants.HASH + skill.getType().toLowerCase().trim();
        String normalizedName = skill.getName() != null
            ? skill.getName().toLowerCase().replaceAll("\\s+", "").trim()
            : "";
        String normalizedCode = skill.getSk() != null
            ? skill.getSk().toLowerCase().replaceAll("\\s+", "").trim()
            : "";
          Map<String, AttributeValue> item = new HashMap<>();
          item.put("pk", AttributeValue.builder().s(skill.getPk()).build());
          item.put("sk", AttributeValue.builder().s(skill.getSk()).build());
          item.put("type", AttributeValue.builder().s(skill.getType()).build());
          item.put("name", AttributeValue.builder().s(skill.getName()).build());
          item.put("tenantType", AttributeValue.builder().s(tenantType).build());
          item.put("normalizedName", normalizedName.isEmpty() ? AttributeValue.builder().nul(true).build() : AttributeValue.builder().s(normalizedName).build());
          item.put("normalizedCode", normalizedCode.isEmpty() ? AttributeValue.builder().nul(true).build() : AttributeValue.builder().s(normalizedCode).build());
          item.put("active", AttributeValue.builder().s(skill.getActive()).build());
          item.put("gsiTypeSk", AttributeValue.builder().s(skill.getGsiTypeSk()).build());
          item.put("effectiveDate", AttributeValue.builder().s(skill.getEffectiveDate()).build());
          item.put("skillCode", AttributeValue.builder().s(skill.getSkillCode()).build());
          item.put("skillName", AttributeValue.builder().s(skill.getSkillName()).build());
          item.put("skillDescription", AttributeValue.builder().s(skill.getSkillDescription()).build());
          item.put("skillType", AttributeValue.builder().s(skill.getSkillType()).build());
          item.put("status", AttributeValue.builder().s(skill.getStatus()).build());
          item.put("skillCategory", AttributeValue.builder().s(skill.getSkillCategory()).build());
          item.put("skillSubCategory", AttributeValue.builder().s(skill.getSkillSubCategory()).build());
          batch.add(item);
          totalRecords++;

          if (batch.size() == BATCH_SIZE) {
              try {
                  batchWrite(batch);
                  log.info("Batch {} uploaded successfully ({} records)", ++totalBatches, BATCH_SIZE);
              } catch (Exception e) {
                  log.error("Error uploading batch {}: {}", totalBatches + 1, e.getMessage());
                  throw new DataBaseException("Error uploading skills batch: " + e.getMessage());
              }
              batch.clear();
          }
      }
      if (!batch.isEmpty()) {
          try {
              batchWrite(batch);
              log.info("Final batch uploaded successfully ({} records)", batch.size());
          } catch (Exception e) {
              log.error("Error uploading final batch: {}", e.getMessage());
              throw new DataBaseException("Error uploading final skills batch: " + e.getMessage());
          }
      }
      log.info("Total records uploaded: {}", totalRecords);
  }

  private void batchWrite(List<Map<String, AttributeValue>> items) {
      List<WriteRequest> writeRequests = new ArrayList<>();
      for (Map<String, AttributeValue> item : items) {
          writeRequests.add(WriteRequest.builder()
                  .putRequest(PutRequest.builder().item(item).build())
                  .build());
      }
      Map<String, List<WriteRequest>> requestItems = new HashMap<>();
      requestItems.put(tableName, writeRequests);

      try {
          BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(builder -> builder.requestItems(requestItems));
          if (!response.unprocessedItems().isEmpty()) {
              log.warn("Some items were not processed in batch write: {}", response.unprocessedItems());
              // Optionally, retry unprocessed items here
          }
      } catch (DynamoDbException e) {
          log.error("DynamoDB batch write error: {}", e.awsErrorDetails().errorMessage());
          throw new DataBaseException("DynamoDB batch write error: " + e.awsErrorDetails().errorMessage());
      }
  }

}
