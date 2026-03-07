package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationListResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.utils.CommonUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class IntegrationDaoImpl implements IntegrationDao {

    private final DynamoDbClient dynamoDbClient;
    private final String integrationTable;

    @Autowired
    public IntegrationDaoImpl(DynamoDBConfig dynamoDBConfig,
                              @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String integrationTableName) {
        this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.integrationTable = integrationTableName;
    }

    @Override
    public IntegrationDto getIntegrationByPartitionKey(String partitionKeyValue) {
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(integrationTable)
                    .keyConditionExpression("pk = :PK")
                    .expressionAttributeValues(Map.of(
                            ":PK", AttributeValue.builder().s(partitionKeyValue).build()
                    ))
                    .build();

            QueryResponse queryResult = dynamoDbClient.query(queryRequest);
            log.info("query result"+String.valueOf(queryResult));
            if (queryResult.count() > 0) {
                List<IntegrationDto.FieldEntry> fields = new ArrayList<>();
                List<IntegrationDto.UniqueIdentifiers> uniqueIdentifiersList = new ArrayList<>();
                List<IntegrationDto.ContentTypeMapping> contentTypeMappingsList = new ArrayList<>();
                List<IntegrationDto.CategoryTypeMapping> categoryTypeMappingsList = new ArrayList<>();
                List<IntegrationDto.CompletionSyncMapping> completionSyncMappingsList = new ArrayList<>();
                List<IntegrationDto.MetaDataMappings> metaDataMappingsList = new ArrayList<>();
                List<IntegrationDto.LessonMetaDataMappings> lessonMetaDataMappingsList = new ArrayList<>();
                IntegrationDto integrationObj = null;
                for (Map<String, AttributeValue> item : queryResult.items()) {
                    IntegrationDto obj = CommonUtil.mapItemToDto(item, IntegrationDto.class);

                    if (item.get("type") != null && Constants.INTEGRATION_FIELD_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.FieldEntry fieldEntry = new IntegrationDto.FieldEntry(obj.getFieldName(), obj.getFieldValue());
                        fields.add(fieldEntry);
                    } else if (item.get("type") != null && Constants.SETTINGS_IDENTIFIERS_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.UniqueIdentifiers uniqueIdentifiers =
                                new IntegrationDto.UniqueIdentifiers(obj.getThirdPartyIdentifier(), obj.getSkillSpringIdentifier());

                        uniqueIdentifiersList.add(uniqueIdentifiers);
                    } else if(item.get("type") != null && Constants.CONTENT_TYPE_MAPPING_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.ContentTypeMapping contentTypeMapping =
                                new IntegrationDto.ContentTypeMapping(obj.getThirdPartyContentType(), obj.getSkillSpringContentType());

                        contentTypeMappingsList.add(contentTypeMapping);
                    } else if(item.get("type") != null && Constants.CATEGORY_TYPE_MAPPING_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.CategoryTypeMapping categoryTypeMapping =
                                new IntegrationDto.CategoryTypeMapping(obj.getThirdPartyCategoryType(), obj.getSkillSpringCategoryType());

                        categoryTypeMappingsList.add(categoryTypeMapping);
                    }
                    else if(item.get("type") != null && Constants.COMPLETION_SYNC_MAPPING_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.CompletionSyncMapping completionSyncMapping =
                                new IntegrationDto.CompletionSyncMapping(obj.getThirdPartyCompletionStatus(), obj.getSkillSpringCompletionStatus());

                        completionSyncMappingsList.add(completionSyncMapping);
                    }
                    else if(item.get("type") != null && Constants.METADATA_MAPPING_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.MetaDataMappings metaDataMappings =
                                new IntegrationDto.MetaDataMappings(obj.getThirdPartyMetadataField(), obj.getSkillSpringMetadataField());
                        metaDataMappingsList.add(metaDataMappings);
                    }
                    else if(item.get("type") != null && Constants.LESSON_METADATA_MAPPING_TYPE.equals(item.get("type").s())) {
                        IntegrationDto.LessonMetaDataMappings lessonMetaDataMapping =
                                new IntegrationDto.LessonMetaDataMappings(obj.getThirdPartyLessonMetadataField(), obj.getSkillSpringLessonMetadataField());
                        lessonMetaDataMappingsList.add(lessonMetaDataMapping);
                    }
                    else {
                        integrationObj = obj;
                    }
                }
                if (integrationObj != null) {
                    integrationObj.setFields(fields);
                    integrationObj.setIdentifiersList(uniqueIdentifiersList);
                    integrationObj.setContentTypeMapping(contentTypeMappingsList);
                    integrationObj.setCategoryTypeMapping(categoryTypeMappingsList);
                    integrationObj.setCompletionSyncMapping(completionSyncMappingsList);
                    integrationObj.setMetaDataMappings(metaDataMappingsList);
                    integrationObj.setLessonMetaDataMappings(lessonMetaDataMappingsList);
                    return integrationObj;
                }
            }
        } catch (DynamoDbException e) {
            log.error("Error reading integration by partitionKey {} from DynamoDB: {}", partitionKeyValue, e.getMessage());
            throw new DataBaseException("Error reading integration by partitionKey " + partitionKeyValue + " from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error reading integration by partitionKey {}: {}", partitionKeyValue, e.getMessage());
            throw new DataBaseException("Unexpected error reading integration by partitionKey " + partitionKeyValue + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public IntegrationListResponse getIntegrations(String sortKey, String order,
                                                   Map<String, String> lastEvaluatedKey,
                                                   int perPage, String status, String integrationType, String searchValue) {

        perPage = perPage < 0 || perPage > 100 ? 10 : perPage;

        String partitionKeyValue = Constants.INTEGRATION_TYPE;

        List<IntegrationDto> itemsToReturn = new ArrayList<>();
        int currentPageSize = 0;
        int limit = perPage;
        Map<String, AttributeValue> lastEvaluatedKeyMap = convertStringMapToAttributeValueMap(lastEvaluatedKey);

        Map<String, AttributeValue> expressionAttributeValues = new java.util.HashMap<>();
        expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());
        String filterExpression = null;
        Map<String, String> expressionAttributeNames = new java.util.HashMap<>();

        if (status != null) {
            filterExpression = "#status = :status";
            expressionAttributeNames.put("#status", "status");
            expressionAttributeValues.put(":status", AttributeValue.builder().s(status).build());
        }
        if (integrationType != null) {
            if (filterExpression != null) {
                filterExpression += " AND #integrationType = :integrationType";
            } else {
                filterExpression = "#integrationType = :integrationType";
            }
            expressionAttributeNames.put("#integrationType", "integrationType");
            expressionAttributeValues.put(":integrationType", AttributeValue.builder().s(integrationType).build());
        }
        if (searchValue != null) {
            if (filterExpression != null) {
                filterExpression += " AND ";
            }
            filterExpression += "contains(#provider, :provider)";
            expressionAttributeNames.put("#provider", "provider");
            expressionAttributeValues.put(":provider", AttributeValue.builder().s(searchValue).build());
        }

        try {
            QueryRequest.Builder countQueryRequestBuilder = QueryRequest.builder()
                    .tableName(integrationTable)
                    .indexName("gsi_type_" + sortKey)
                    .keyConditionExpression("#type = :PK")
                    .expressionAttributeNames(Map.of("#type", "type"))
                    .expressionAttributeValues(expressionAttributeValues)
                    .select("COUNT");

            if (filterExpression != null) {
                countQueryRequestBuilder.filterExpression(filterExpression)
                        .expressionAttributeNames(expressionAttributeNames);
            }

            QueryResponse countQueryResult = dynamoDbClient.query(countQueryRequestBuilder.build());
            int count = countQueryResult.count();

            do {
                QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
                        .tableName(integrationTable)
                        .indexName("gsi_type_" + sortKey)
                        .scanIndexForward(!order.equalsIgnoreCase("desc"))
                        .keyConditionExpression("#type = :PK")
                        .expressionAttributeNames(Map.of("#type", "type"))
                        .expressionAttributeValues(expressionAttributeValues)
                        .limit(limit);

                if (lastEvaluatedKeyMap != null) {
                    queryRequestBuilder.exclusiveStartKey(lastEvaluatedKeyMap);
                }
                if (filterExpression != null) {
                    queryRequestBuilder.filterExpression(filterExpression)
                            .expressionAttributeNames(expressionAttributeNames);
                }

                QueryResponse queryResult = dynamoDbClient.query(queryRequestBuilder.build());
                List<IntegrationDto> integrations = queryResult.items().stream()
                        .map(item -> CommonUtil.mapItemToDto(item, IntegrationDto.class))
                        .toList();

                itemsToReturn.addAll(integrations);
                currentPageSize += queryResult.count();
                lastEvaluatedKeyMap = queryResult.lastEvaluatedKey();

                if (currentPageSize < perPage && lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty()) {
                    limit = perPage - currentPageSize;
                } else {
                    break;
                }
            } while (currentPageSize < perPage && lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty());
            return new IntegrationListResponse(itemsToReturn, lastEvaluatedKeyMap, count);
        } catch (DynamoDbException e) {
            log.error("Error reading from DynamoDB {} : ", e.getMessage());
            throw new DataBaseException("Error reading from Integrations table in DynamoDB " + e.getMessage());
        }
    }

    private void addAttributeValue(Map<String, AttributeValue> map, String key, String value) {
        map.put(key, AttributeValue.builder().s(value != null ? value : "").build());
    }

    @Override
    public void saveGeneralInformation(String pk, String sk, String type, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.GeneralInformation generalInformation = request.getGeneralInformation();
        boolean isNew = !itemExists(pk, sk);

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();
        String emailId = UserContext.getUserEmail();

        String updateExpression = "SET lmsIntegrationId = :lmsIntegrationId, provider = :provider, #type = :type, integrationType = :integrationType, " +
                "integrationId = :integrationId, #status = :status, integrationOwner = :integrationOwner,pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, emailId = :emailId";
        if (generalInformation.getReasonForChange() != null) {
            updateExpression += ", reasonForChange = :reasonForChange";
        }
        if (isNew) {
            updateExpression += ", createdOn = :createdOn, createdBy = :createdBy";
        }

        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#type", "type");
        expressionAttributeNames.put("#status", "status");


        addAttributeValue(values, ":lmsIntegrationId", request.getLmsIntegrationId());
        addAttributeValue(values, ":provider", generalInformation.getProvider());
        addAttributeValue(values, ":integrationType", request.getIntegrationType());
        addAttributeValue(values, ":integrationId", generalInformation.getIntegrationId());
        addAttributeValue(values, ":status", status);
        addAttributeValue(values, ":type", type);     // Set from backend
        addAttributeValue(values, ":integrationOwner", generalInformation.getIntegrationOwner());
        addAttributeValue(values, ":pageName", request.getPageName());

        // Auditing fields
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":emailId", emailId);
        if (generalInformation.getReasonForChange() != null) {
            addAttributeValue(values, ":reasonForChange", generalInformation.getReasonForChange());
        }
        if (isNew) {
            addAttributeValue(values, ":createdOn", now);
            addAttributeValue(values, ":createdBy", userName);
        }

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
    }

    // Helper method to check if item exists
    private boolean itemExists(String pk, String sk) {
        Map<String, AttributeValue> key = Map.of(
                "pk", AttributeValue.fromS(pk),
                "sk", AttributeValue.fromS(sk)
        );
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(integrationTable)
                .key(key)
                .build();
        GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
        return response.hasItem() && !response.item().isEmpty();
    }

    @Override
    public void saveCoreConfiguration(String pk, String sk, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.CoreConfiguration coreConfiguration = request.getCoreConfiguration();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();

        String updateExpression = "SET hostName = :hostName, clientId = :clientId, organizationId = :organizationId, clientSecret = :clientSecret, " +
                "pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, testConnection = :testConnection, #status = :status";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":hostName", coreConfiguration.getHostName());
        addAttributeValue(values, ":clientId", coreConfiguration.getClientId());
        addAttributeValue(values, ":organizationId", coreConfiguration.getOrganizationId());
        addAttributeValue(values, ":clientSecret", coreConfiguration.getClientSecret());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":testConnection", coreConfiguration.getTestConnection());
        addAttributeValue(values, ":status", status);

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
        saveCoreConfigurationAdditionalFields(pk, coreConfiguration.getFields());
    }

    @Override
    public void saveSettings(String pk, String sk, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.Settings settings = request.getSettings();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();

        String updateExpression = "SET authenticationMethod = :authenticationMethod, syncType = :syncType, " +
                "syncSchedule = :syncSchedule, weekDay = :weekDay, syncTime = :syncTime,pageName=:pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, #status = :status";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":authenticationMethod", settings.getAuthenticationMethod());
        addAttributeValue(values, ":syncType", settings.getSyncType());
        addAttributeValue(values, ":syncSchedule", settings.getSyncSchedule());
        addAttributeValue(values, ":weekDay", settings.getWeekDay());
        addAttributeValue(values, ":syncTime", settings.getSyncTime());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":status", status);

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
        saveSettingsConfigurationAdditionalFields(pk, settings.getIdentifiersList());

    }

    @Override
    public void saveMetaDataMapping(String partitionKey, String sortKey, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.MetaData metaData= request.getMetaData();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();
        String updateExpression = "SET prefix = :prefix, pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, #status = :status";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":prefix", metaData.getPrefix());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":status", status);
        updateItem(partitionKey, sortKey, updateExpression, values, expressionAttributeNames);
        saveMetaDataFieldMapping(partitionKey, metaData.getMetaDataMappings());
    }

    @Override
    public void saveLessonMapping(String partitionKey, String sortKey, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.MetaData metaData= request.getMetaData();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();
        String updateExpression = "SET prefix = :prefix, pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, #status = :status";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":prefix", metaData.getPrefix());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":status", status);
        updateItem(partitionKey, sortKey, updateExpression, values, expressionAttributeNames);
       saveLessonMetaDataFieldMapping(partitionKey,metaData.getLessonMetaDataMappings());

    }

    public void saveCoreConfigurationAdditionalFields(String pk, List<IntegrationDraftRequest.FieldEntry> fields) {
        if (fields == null || fields.isEmpty()) {
            log.warn("No fields to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.FieldEntry field : fields) {
            if (field.getFieldName() == null || field.getFieldValue() == null) {
                log.warn("Skipping field with null name or value for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.INTEGRATION_FIELD_PREFIX + field.getFieldName()));
            item.put("type", AttributeValue.fromS(Constants.INTEGRATION_FIELD_TYPE));
            item.put("fieldName", AttributeValue.fromS(field.getFieldName()));
            item.put("fieldValue", AttributeValue.fromS(field.getFieldValue()));

            putItem(item);
        }
    }

    public void saveSettingsConfigurationAdditionalFields(String pk, List<IntegrationDraftRequest.UniqueIdentifiers> uniqueIdentifiersList) {
        if ( uniqueIdentifiersList== null || uniqueIdentifiersList.isEmpty()) {
            log.warn("No fields to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.UniqueIdentifiers uniqueIdentifiers: uniqueIdentifiersList) {
            if (uniqueIdentifiers.getThirdPartyIdentifier() == null ) {
                log.warn("Skipping unique identifier with null name or value for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.SETTINGS_IDENTIFIERS_PREFIX + uniqueIdentifiers.getThirdPartyIdentifier()));
            item.put("type", AttributeValue.fromS(Constants.SETTINGS_IDENTIFIERS_TYPE));
            item.put("thirdPartyIdentifier", AttributeValue.fromS(uniqueIdentifiers.getThirdPartyIdentifier()));
            item.put("skillSpringIdentifier", AttributeValue.fromS(uniqueIdentifiers.getSkillSpringIdentifier()));
            putItem(item);
        }
    }

    public void saveMetaDataFieldMapping(String pk, List<IntegrationDraftRequest.MetaDataMappings> metaDataMappings) {
        if (metaDataMappings == null || metaDataMappings.isEmpty()) {
            log.warn("No metadata mappings to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.MetaDataMappings mapping : metaDataMappings) {
            if (mapping.getThirdPartyMetadataField() == null || mapping.getSkillSpringMetadataField() == null) {
                log.warn("Skipping metadata mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.METADATA_MAPPING_PREFIX + mapping.getThirdPartyMetadataField()));
            item.put("type", AttributeValue.fromS(Constants.METADATA_MAPPING_TYPE));
            item.put("thirdPartyMetadataField", AttributeValue.fromS(mapping.getThirdPartyMetadataField()));
            item.put("skillSpringMetadataField", AttributeValue.fromS(mapping.getSkillSpringMetadataField()));

            putItem(item);
        }
    }
    public void saveLessonMetaDataFieldMapping(String pk, List<IntegrationDraftRequest.LessonMetaDataMappings> lessonMetaDataMappings) {
        if (lessonMetaDataMappings== null || lessonMetaDataMappings.isEmpty()) {
            log.warn("No metadata mappings to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.LessonMetaDataMappings mapping : lessonMetaDataMappings) {
            if (mapping.getThirdPartyLessonMetadataField() == null || mapping.getSkillSpringLessonMetadataField() == null) {
                log.warn("Skipping lesson metadata mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.LESSON_METADATA_MAPPING_PREFIX + mapping.getThirdPartyLessonMetadataField()));
            item.put("type", AttributeValue.fromS(Constants.LESSON_METADATA_MAPPING_TYPE));
            item.put("thirdPartyLessonMetadataField", AttributeValue.fromS(mapping.getThirdPartyLessonMetadataField()));
            item.put("skillSpringLessonMetadataField", AttributeValue.fromS(mapping.getSkillSpringLessonMetadataField()));
            putItem(item);
        }
    }

    @Override
    public void saveContentMapping(String pk, String sk, String status, IntegrationDraftRequest request) {
        IntegrationDraftRequest.ContentMapping contentMapping = request.getContentMapping();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();
        String updateExpression = "SET categoryMappingType = :categoryMappingType, categoryName = :categoryName, " +
                "pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, #status = :status";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":categoryMappingType", contentMapping.getCategoryMappingType());
        addAttributeValue(values, ":categoryName", contentMapping.getCategoryName());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":status", status);

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
        saveContentTypeMapping(pk, contentMapping.getContentTypeMapping());
        saveCategoryTypeMapping(pk, contentMapping.getCategoryTypeMapping());
        saveCompletionSyncMapping(pk, contentMapping.getCompletionSyncMapping());
    }

    public void saveContentTypeMapping(String pk, List<IntegrationDraftRequest.ContentTypeMapping> contentTypeMapping) {
        if (contentTypeMapping == null || contentTypeMapping.isEmpty()) {
            log.warn("No content type mappings to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.ContentTypeMapping mapping : contentTypeMapping) {
            if (mapping.getThirdPartyContentType() == null || mapping.getSkillSpringContentType() == null) {
                log.warn("Skipping content type mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.CONTENT_TYPE_MAPPING_PREFIX + mapping.getThirdPartyContentType()));
            item.put("type", AttributeValue.fromS(Constants.CONTENT_TYPE_MAPPING_TYPE));
            item.put("thirdPartyContentType", AttributeValue.fromS(mapping.getThirdPartyContentType()));
            item.put("skillSpringContentType", AttributeValue.fromS(mapping.getSkillSpringContentType()));

            putItem(item);
        }
    }

    public void saveCategoryTypeMapping(String pk, List<IntegrationDraftRequest.CategoryTypeMapping> categoryTypeMapping) {
        if (categoryTypeMapping == null || categoryTypeMapping.isEmpty()) {
            log.warn("No category type mappings to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.CategoryTypeMapping mapping : categoryTypeMapping) {
            if (mapping.getThirdPartyCategoryType() == null || mapping.getSkillSpringCategoryType() == null) {
                log.warn("Skipping category type mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.CATEGORY_TYPE_MAPPING_PREFIX + mapping.getThirdPartyCategoryType()));
            item.put("type", AttributeValue.fromS(Constants.CATEGORY_TYPE_MAPPING_TYPE));
            item.put("thirdPartyCategoryType", AttributeValue.fromS(mapping.getThirdPartyCategoryType()));
            item.put("skillSpringCategoryType", AttributeValue.fromS(mapping.getSkillSpringCategoryType()));

            putItem(item);
        }
    }

    public void saveCompletionSyncMapping(String pk, List<IntegrationDraftRequest.CompletionSyncMapping> completionSyncMapping) {
        if (completionSyncMapping == null || completionSyncMapping.isEmpty()) {
            log.warn("No completion sync mappings to save for pk: {}", pk);
            return;
        }
        for (IntegrationDraftRequest.CompletionSyncMapping mapping : completionSyncMapping) {
            if (mapping.getThirdPartyCompletionStatus() == null || mapping.getSkillSpringCompletionStatus() == null) {
                log.warn("Skipping completion sync mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.COMPLETION_SYNC_MAPPING_PREFIX + mapping.getThirdPartyCompletionStatus()));
            item.put("type", AttributeValue.fromS(Constants.COMPLETION_SYNC_MAPPING_TYPE));
            item.put("thirdPartyCompletionStatus", AttributeValue.fromS(mapping.getThirdPartyCompletionStatus()));
            item.put("skillSpringCompletionStatus", AttributeValue.fromS(mapping.getSkillSpringCompletionStatus()));

            putItem(item);
        }
    }

    private void putItem(Map<String, AttributeValue> item) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(integrationTable)
                .item(item)
                .build();
        dynamoDbClient.putItem(request);
    }

    void updateItem(String pk, String sk, String updateExpression, Map<String, AttributeValue> values, Map<String, String> expressionAttributeNames) {
        Map<String, AttributeValue> key = Map.of(
                "pk", AttributeValue.fromS(pk),
                "sk", AttributeValue.fromS(sk)
        );
        UpdateItemRequest.Builder requestBuilder = UpdateItemRequest.builder()
                .tableName(integrationTable)
                .key(key)
                .updateExpression(updateExpression)
                .expressionAttributeValues(values);
        if (expressionAttributeNames != null && !expressionAttributeNames.isEmpty()) {
            requestBuilder.expressionAttributeNames(expressionAttributeNames);
        }
        dynamoDbClient.updateItem(requestBuilder.build());
    }

    private Map<String, AttributeValue> convertStringMapToAttributeValueMap(Map<String, String> stringMap) {
        if (stringMap == null) {
            return null;
        }
        Map<String, AttributeValue> attributeValueMap = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            attributeValueMap.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
        }
        return attributeValueMap;
    }

  @Override
  public void saveSFTPConfiguration(String pk, String sk, String type, String status, SFTPIntegrationReqDto request) {
    SFTPIntegrationReqDto.Configuration configuration = request.getConfiguration();
    boolean isNew = !itemExists(pk, sk);

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String now = ZonedDateTime.now().format(outputFormatter);
    String userName = UserContext.getCreatedBy();
    String emailId = UserContext.getUserEmail();

    String updateExpression = "SET lmsIntegrationId = :lmsIntegrationId, provider = :provider, " +
        " #type = :type, integrationType = :integrationType, " +
        " syncType = :syncType, #status = :status, #location = :location," +
        " #userName = :userName, #versionStatus = :versionStatus," +
        " #password = :password, #host = :host, #port = :port, #uniqIntegrationKey = :uniqIntegrationKey," +
        " pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, emailId = :emailId, testConnection = :testConnection";

    if (isNew) {
      updateExpression += ", createdOn = :createdOn, createdBy = :createdBy";
    }

    Map<String, AttributeValue> values = new HashMap<>();
    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#type", "type");
    expressionAttributeNames.put("#status", "status");
    expressionAttributeNames.put("#location", "location");
    expressionAttributeNames.put("#userName", "userName");
    expressionAttributeNames.put("#password", "password");
    expressionAttributeNames.put("#host", "host");
    expressionAttributeNames.put("#port", "port");
    expressionAttributeNames.put("#uniqIntegrationKey", "uniqIntegrationKey");
    expressionAttributeNames.put("#versionStatus", "versionStatus");


    addAttributeValue(values, ":lmsIntegrationId", request.getLmsIntegrationId());
    addAttributeValue(values, ":integrationType", request.getIntegrationType());
    addAttributeValue(values, ":status", status);
    addAttributeValue(values, ":type", type);
    addAttributeValue(values, ":pageName", request.getPageName());
    addAttributeValue(values, ":provider", configuration.getProvider());
    addAttributeValue(values, ":syncType", configuration.getSyncType());
    addAttributeValue(values, ":location", configuration.getLocation());
    addAttributeValue(values, ":userName", configuration.getUserName());
    addAttributeValue(values, ":password", configuration.getPassword());
    addAttributeValue(values, ":host", configuration.getHost());
    addAttributeValue(values, ":port", configuration.getPort());
    addAttributeValue(values, ":testConnection", configuration.getTestConnection());

    // Auditing fields
    addAttributeValue(values, ":updatedDate", now);
    addAttributeValue(values, ":updatedBy", userName);
    addAttributeValue(values, ":emailId", emailId);
    addAttributeValue(values, ":uniqIntegrationKey", request.getUniqIntegrationKey());
    addAttributeValue(values, ":versionStatus", request.getVersionStatus());
    if (isNew) {
      addAttributeValue(values, ":createdOn", now);
      addAttributeValue(values, ":createdBy", userName);
    }

    updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
  }

    @Override
    public SFTPIntegrationResponseDto getSFTPIntegrationByPartitionKey(String partitionKeyValue, boolean edit) {
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(integrationTable)
                .keyConditionExpression("pk = :PK")
                .expressionAttributeValues(Map.of(
                    ":PK", AttributeValue.builder().s(partitionKeyValue).build()
                ))
                .build();

            QueryResponse queryResult = dynamoDbClient.query(queryRequest);
            if (queryResult.count() > 0) {
                List<SFTPIntegrationResponseDto.SftpCategoryTypeMapping> categoryTypeMappingsList = new ArrayList<>();
                SFTPIntegrationResponseDto obj = null;
                for (Map<String, AttributeValue> item : queryResult.items()) {
                    SFTPIntegrationResponseDto integrationObj = CommonUtil.mapItemToDto(item, SFTPIntegrationResponseDto.class);
                    if (item.get("type") != null && Constants.SFTP_CATEGORY_TYPE_MAPPING_TYPE.equals(item.get("type").s())) {
                        SFTPIntegrationResponseDto.SftpCategoryTypeMapping categoryTypeMapping =
                                new SFTPIntegrationResponseDto.SftpCategoryTypeMapping(
                                        integrationObj.getThirdPartyCategoryType(),
                                        integrationObj.getSkillSpringCategoryType()
                                );
                        categoryTypeMappingsList.add(categoryTypeMapping);
                    } else {
                        obj = integrationObj;
                    }
                }
                if (obj != null) {
                    obj.setSftpCategoryTypeMapping(categoryTypeMappingsList);
                    return obj;
                }
            }
        } catch (DynamoDbException e) {
            log.error("Error fetching SFTP integration for partitionKey {}: {}", partitionKeyValue, e.getMessage());
            throw new DataBaseException("Error fetching SFTP integration for partitionKey: " + partitionKeyValue + " - " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveSftpSettings(String pk, String sk, String status, SFTPIntegrationReqDto request) {

        SFTPIntegrationReqDto.SftpSettingsDTO settings = request.getSftpSettingsDTO();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();

        String updateExpression = "SET syncTime = :syncTime, pageName= :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();

        addAttributeValue(values, ":syncTime", settings.getSyncTime());
        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
    }

    @Override
    public void saveSftpCategoryMapping(String partitionKey, String sortKey, String status, SFTPIntegrationReqDto request){
        SFTPIntegrationReqDto.CategoryMapping categoryMapping = request.getCategoryMapping();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();
        String updateExpression = "SET pageName = :pageName, updatedDate = :updatedDate, updatedBy = :updatedBy, #status = :status, #versionStatus = :versionStatus";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");
        expressionAttributeNames.put("#versionStatus", "versionStatus");

        addAttributeValue(values, ":pageName", request.getPageName());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);
        addAttributeValue(values, ":status", status);
        addAttributeValue(values, ":versionStatus", request.getVersionStatus());
        updateItem(partitionKey, sortKey, updateExpression, values, expressionAttributeNames);
        saveSftpCategoryTypeMapping(partitionKey, categoryMapping.getCategoryTypeMappings());
    }

    public void saveSftpCategoryTypeMapping(String pk, List<SFTPIntegrationReqDto.SftpCategoryTypeMapping> sftpCategoryTypeMappings){
        if (sftpCategoryTypeMappings == null || sftpCategoryTypeMappings.isEmpty()) {
            log.warn("No Sftp category type mappings to save for pk: {}", pk);
            return;
        }
        for (SFTPIntegrationReqDto.SftpCategoryTypeMapping mapping : sftpCategoryTypeMappings) {
            if (mapping.getThirdPartyCategoryType() == null || mapping.getSkillSpringCategoryType() == null) {
                log.warn("Skipping Sftp category type mapping with null fields for pk: {}", pk);
                continue;
            }
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.fromS(pk));
            item.put("sk", AttributeValue.fromS(Constants.SFTP_CATEGORY_TYPE_MAPPING_PREFIX + mapping.getThirdPartyCategoryType()));
            item.put("type", AttributeValue.fromS(Constants.SFTP_CATEGORY_TYPE_MAPPING_TYPE));
            item.put("thirdPartyCategoryType", AttributeValue.fromS(mapping.getThirdPartyCategoryType()));
            item.put("skillSpringCategoryType", AttributeValue.fromS(mapping.getSkillSpringCategoryType()));

            putItem(item);
        }
    }

    @Override
    public IntegrationListResponse fetchExistingIntegration(String sortKey, String status, String integrationType) {

        List<IntegrationDto> itemsToReturn = new ArrayList<>();
        Map<String, AttributeValue> expressionAttributeValues = new java.util.HashMap<>();
        Map<String, String> expressionAttributeNames = new java.util.HashMap<>();
        expressionAttributeNames.put("#integrationType", "integrationType");
        expressionAttributeNames.put("#type", "type");
        expressionAttributeNames.put("#status", "status");
        expressionAttributeValues.put(":integrationType", AttributeValue.builder().s(integrationType).build());
        expressionAttributeValues.put(":type", AttributeValue.builder().s(Constants.INTEGRATION_TYPE).build());
        expressionAttributeValues.put(":status", AttributeValue.builder().s(status).build());

        try {
            QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
                    .tableName(integrationTable)
                    .indexName("gsi_type_" + sortKey)
                    .keyConditionExpression("#type = :type")
                    .filterExpression("#integrationType = :integrationType AND #status = :status")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .projectionExpression("provider, uniqIntegrationKey, lmsIntegrationId");

            QueryResponse queryResult = dynamoDbClient.query(queryRequestBuilder.build());
            Map<String, AttributeValue> lastEvaluatedKeyMap = queryResult.lastEvaluatedKey();
            List<IntegrationDto> integrations = queryResult.items().isEmpty() ? new ArrayList<>() : queryResult.items().stream()
                    .map(item -> CommonUtil.mapItemToDto(item, IntegrationDto.class))
                    .toList();

            itemsToReturn.addAll(integrations);
            return new IntegrationListResponse(itemsToReturn, lastEvaluatedKeyMap, itemsToReturn.size());
        } catch (DynamoDbException e) {
            log.error("Error reading from DynamoDB {} : ", e.getMessage());
            throw new DataBaseException("Error reading from Integrations table in DynamoDB " + e.getMessage());
        }
    }

    @Override
    public void updateSFTPConfiguration(String pk, String sk, String status, SFTPIntegrationReqDto request) {

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String now = ZonedDateTime.now().format(outputFormatter);
        String userName = UserContext.getCreatedBy();

        String updateExpression = "SET reasonForChange = :reasonForChange, provider = :provider, #status= :status, updatedDate = :updatedDate, updatedBy = :updatedBy";
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");

        addAttributeValue(values, ":reasonForChange", request.getReasonForChange());
        addAttributeValue(values, ":provider", request.getConfiguration().getProvider());
        addAttributeValue(values, ":status", request.getStatus());
        addAttributeValue(values, ":updatedDate", now);
        addAttributeValue(values, ":updatedBy", userName);

        updateItem(pk, sk, updateExpression, values, expressionAttributeNames);
    }
}
