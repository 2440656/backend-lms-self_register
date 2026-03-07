package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.yaml.snakeyaml.scanner.Constant;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class UserSettingsDaoImpl implements UserSettingsDao{

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final DynamoDbTable<UserSettings> userTable;

    public UserSettingsDaoImpl(
            DynamoDBConfig dynamoDBConfig,
            @Value("${AWS_DYNAMODB_USER_TABLE_NAME}") String tableName
    ) {
        dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.tableName = tableName;
        this.userTable = dynamoDBConfig.getDynamoDBEnhancedClient()
                .table(tableName, TableSchema.fromBean(UserSettings.class));
    }

    @Override
    public UserSettings getUserSettingsByEmailId(String emailId, String sk) {

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":emailId", AttributeValue.builder().s(emailId).build());
        expressionValues.put(":sk", AttributeValue.builder().s(sk).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName(Constants.GSI_EMAIL_ID_SK)
                .keyConditionExpression("emailId = :emailId AND sk = :sk")
                .expressionAttributeValues(expressionValues)
                .limit(1)
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (response.items().isEmpty()) {
            log.info("No UserSettings found for emailId: {} and sk: {}", emailId, sk);
            return null;
        }

        return mapToUserSettings(response.items().getFirst());
    }

    @Override
    public void saveUserSettings(UserSettings userSettings) {
        userTable.putItem(userSettings);
    }

    @Override
    public Map<String,AttributeValue> updateEmptyTypeFields(Map<String, String> lastEvaluatedKey, int limit) {
        int scanLimit = (limit > 0) ? limit : 100;

        Map<String, AttributeValue> lastEvaluatedKeyMap =
                (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty())
                        ? convertStringMapToAttributeValueMap(lastEvaluatedKey)
                        : null;

        ScanRequest.Builder scanBuilder = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("attribute_not_exists(#type) OR #type = :empty")
                .expressionAttributeNames(Map.of("#type", "type"))
                .expressionAttributeValues(Map.of(":empty", AttributeValue.builder().s("").build()))
                .limit(scanLimit);

        if (lastEvaluatedKeyMap != null && !lastEvaluatedKeyMap.isEmpty()) {
            scanBuilder.exclusiveStartKey(lastEvaluatedKeyMap);
        }

        ScanResponse scanResponse = dynamoDbClient.scan(scanBuilder.build());
        List<Map<String, AttributeValue>> items = scanResponse.items();

        for (int i = 0; i < items.size(); i += 25) {
            List<WriteRequest> writeRequests = new ArrayList<>();
            for (int j = i; j < i + 25 && j < items.size(); j++) {
                Map<String, AttributeValue> item = new HashMap<>(items.get(j));
                item.put("type", AttributeValue.builder().s("user").build());
                writeRequests.add(WriteRequest.builder()
                        .putRequest(PutRequest.builder().item(item).build())
                        .build());
            }
            if (!writeRequests.isEmpty()) {
                dynamoDbClient.batchWriteItem(b -> b.requestItems(Map.of(tableName, writeRequests)));
            }
        }

        Map<String, AttributeValue> newLastEvaluatedKey = scanResponse.lastEvaluatedKey();
        String lastEvaluatedKeyStr = null;
        if (newLastEvaluatedKey != null && !newLastEvaluatedKey.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                lastEvaluatedKeyStr = mapper.writeValueAsString(newLastEvaluatedKey);
            } catch (Exception e) {
                log.error("Failed to serialize lastEvaluatedKey", e);
            }
        }
        return newLastEvaluatedKey;
    }

    private UserSettings mapToUserSettings(Map<String, AttributeValue> item) {
        UserSettings settings = new UserSettings();
        if (item.containsKey("pk")) settings.setPk(item.get("pk").s());
        if (item.containsKey("sk")) settings.setSk(item.get("sk").s());
        if (item.containsKey("tenantCode")) settings.setTenantCode(item.get("tenantCode").s());
        if (item.containsKey("type")) settings.setType(item.get("type").s());
        if (item.containsKey("voiceId")) settings.setVoiceId(item.get("voiceId").s());
        if (item.containsKey("theme")) settings.setTheme(item.get("theme").s());
        if (item.containsKey("createdBy")) settings.setCreatedBy(item.get("createdBy").s());
        if (item.containsKey("createdDate")) settings.setCreatedDate(item.get("createdDate").s());
        if (item.containsKey("updatedBy")) settings.setUpdatedBy(item.get("updatedBy").s());
        if (item.containsKey("updatedDate")) settings.setUpdatedDate(item.get("updatedDate").s());
        if ( item.containsKey("emailId")) settings.setEmailId(item.get("emailId").s());
        return settings;
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
