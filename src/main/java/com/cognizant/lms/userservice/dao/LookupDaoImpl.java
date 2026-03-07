package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.utils.LogUtil;
import com.cognizant.lms.userservice.utils.TenantUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class LookupDaoImpl implements LookupDao {

    private final DynamoDbClient dynamoDbClient;
    private final String lookupTable;

    @Autowired
    public LookupDaoImpl(DynamoDBConfig dynamoDBConfig,
                         @Value("${AWS_DYNAMODB_LOOKUP_TABLE_NAME}") String lookupTableName){

        this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.lookupTable = lookupTableName;
    }

    @Override
    public List<LookupDto> getLookupData(String type, String skSuffix) {
        log.info(LogUtil.getLogInfo("GET_LOOKUP", "IN_PROGRESS") + "Fetching data for type: {}, skSuffix: {}", type, skSuffix);
        List<LookupDto> lookupDtoList = new ArrayList<>();
        String partitionKeyValue = "LOOKUP#" + type;
        try {
            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(lookupTable)
                    .scanIndexForward(true); // Ensures alphabetical order

            // Initialize expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());

            if (skSuffix == null) {
                // Only type is given, use GSI
                queryBuilder.indexName(Constants.LOOKUP_INDEX_NAME);
                queryBuilder.keyConditionExpression("pk = :PK");
            } else {
                // Both type and skSuffix are given, query base table
                String skValue = "CITY#" + skSuffix;
                queryBuilder.keyConditionExpression("pk = :PK and begins_with(sk, :SK)");
                expressionAttributeValues.put(":SK", AttributeValue.builder().s(skValue).build());
            }

            // Set expression attribute values
            queryBuilder.expressionAttributeValues(expressionAttributeValues);

            QueryResponse queryResponse = dynamoDbClient.query(queryBuilder.build());

            lookupDtoList = queryResponse.items().stream()
                    .map(this::mapItemToLookupDto)
                    .collect(Collectors.toList());

        } catch (DynamoDbException e) {
            log.error(LogUtil.getLogError("GET_LOOKUP",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    "FAILED") + "Error reading data from DynamoDB {} : ", e.getMessage(), e.getStackTrace());
        }
        return lookupDtoList;
    }

    @Override
    public List<LookupDto> getServiceLineLookupData(String type, String skSuffix) {
        log.info(LogUtil.getLogInfo("GET_LOOKUP", "IN_PROGRESS") + "Fetching data for type: {}, skSuffix: {}", type, skSuffix);
        List<LookupDto> lookupDtoList = new ArrayList<>();
        String partitionKeyValue = TenantUtil.getTenantCode() + Constants.HASH  + type;
        try {
            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(lookupTable)
                    .scanIndexForward(true); // Ensures alphabetical order

            // Initialize expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":PK", AttributeValue.builder().s(partitionKeyValue).build());

            if (skSuffix == null) {
                // Only type is given, use GSI
                queryBuilder.indexName(Constants.LOOKUP_INDEX_NAME);
                queryBuilder.keyConditionExpression("pk = :PK");
            } else {
                // Both type and skSuffix are given, query base table
                String skValue = Constants.SERVICELINE_PREFIX + skSuffix;
                queryBuilder.keyConditionExpression("pk = :PK and begins_with(sk, :SK)");
                expressionAttributeValues.put(":SK", AttributeValue.builder().s(skValue).build());
            }

            // Set expression attribute values
            queryBuilder.expressionAttributeValues(expressionAttributeValues);

            QueryResponse queryResponse = dynamoDbClient.query(queryBuilder.build());

            lookupDtoList = queryResponse.items().stream()
                    .map(this::mapItemToLookupDto)
                    .collect(Collectors.toList());

        } catch (DynamoDbException e) {
            log.error(LogUtil.getLogError("GET_LOOKUP",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    "FAILED") + "Error reading data from DynamoDB {} : ", e.getMessage(), e.getStackTrace());
        }
        return lookupDtoList;
    }


    private LookupDto mapItemToLookupDto(Map<String, AttributeValue> item){
        return new LookupDto(
                getStringValue(item, "pk"),
                getStringValue(item, "sk"),
                getStringValue(item, "name")
        );
    }

    private String getStringValue(Map<String, AttributeValue> item, String key){
        return item.containsKey(key) && item.get(key) != null ? item.get(key).s() : null;
    }

    @Override
    public List<AiVoicePreviewLookupDto> getAiVoicePreviewData() {
        String pkValue = "t-2#voice";
        Map<String, String> expressionAttrNames = new HashMap<>();
        expressionAttrNames.put("#pk", "pk");
        Map<String, AttributeValue> expressionAttrValues = new HashMap<>();
        expressionAttrValues.put(":pkVal", AttributeValue.builder().s(pkValue).build());
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(lookupTable)
                .keyConditionExpression("#pk = :pkVal")
                .filterExpression("attribute_exists(fileName)")
                .expressionAttributeNames(expressionAttrNames)
                .expressionAttributeValues(expressionAttrValues)
                .build();
        List<AiVoicePreviewLookupDto> result = new ArrayList<>();
        try {
            QueryResponse response = dynamoDbClient.query(queryRequest);
            for (Map<String, AttributeValue> item : response.items()) {
                result.add(new AiVoicePreviewLookupDto(
                        item.getOrDefault("pk", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("sk", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("fileName", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("gender", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("language", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("path", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("voiceId", AttributeValue.builder().s(null).build()).s(),
                        item.getOrDefault("voiceName", AttributeValue.builder().s(null).build()).s(),
                        null
                ));
            }
        } catch (DynamoDbException e) {
            log.error("Error fetching AI voice preview data: {}", e.getMessage(), e);
        }
        return result;
    }
}
