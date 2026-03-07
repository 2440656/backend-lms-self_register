package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class LocaleDataDaoImpl implements LocaleDataDao {

    private static final Logger logger = LoggerFactory.getLogger(LocaleDataDaoImpl.class);

    private final DynamoDbClient dynamoDbClient;
    private final String localeTable;

    @Autowired
    public LocaleDataDaoImpl(DynamoDBConfig dynamoDBConfig,
                         @Value("${LOCALE_DATA_TABLE_NAME}") String localeTable){

        this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.localeTable = localeTable;
        logger.debug("Initialized LocaleDataDaoImpl with table: {}", localeTable);
    }

    @Override
    public void saveLocaleData(LocaleRequestDTO localeRequestDTO) {
        logger.info("Saving locale data for languageCode={}, pageName={}", localeRequestDTO.getLanguageCode(), localeRequestDTO.getPageName());
        try {
            // Use languageCode as hash key and pageName as range key
            String languageCode = localeRequestDTO.getLanguageCode();
            String pageName = localeRequestDTO.getPageName();

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("languageCode", AttributeValue.builder().s(languageCode).build());
            item.put("pageName", AttributeValue.builder().s(pageName).build());
            item.put("localeData", AttributeValue.builder().s(localeRequestDTO.getLocaleData() != null ? localeRequestDTO.getLocaleData() : "").build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(localeTable)
                .item(item)
                .build();

            dynamoDbClient.putItem(putItemRequest);
            logger.debug("Saved locale item languageCode={}, pageName={} to table {}", languageCode, pageName, localeTable);
        } catch (DynamoDbException e) {
            logger.error("Error saving locale data to DynamoDB: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataForList() {
        logger.info("Fetching all locale data list from table: {}", localeTable);
        List<LocaleRequestDTO> results = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        try {
            do {
                ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(localeTable)
                    .exclusiveStartKey(lastEvaluatedKey)
                    .build();

                ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
                List<Map<String, AttributeValue>> items = scanResponse.items();
                if (items != null) {
                    for (Map<String, AttributeValue> item : items) {
                        results.add(mapToLocaleRequestDTO(item));
                    }
                }
                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());
        } catch (DynamoDbException e) {
            logger.error("Error scanning locale table {}: {}", localeTable, e.getMessage(), e);
            throw e;
        }

        return results;
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataByLanguageCode(String languageCode) {
        logger.info("Fetching locale data for languageCode={} from table: {}", languageCode, localeTable);
        List<LocaleRequestDTO> results = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        try {
            do {
                QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(localeTable)
                    .keyConditionExpression("languageCode = :lc")
                    .expressionAttributeValues(Map.of(":lc", AttributeValue.builder().s(languageCode).build()))
                    .exclusiveStartKey(lastEvaluatedKey)
                    .build();

                QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
                List<Map<String, AttributeValue>> items = queryResponse.items();
                if (items != null) {
                    for (Map<String, AttributeValue> item : items) {
                        results.add(mapToLocaleRequestDTO(item));
                    }
                }
                lastEvaluatedKey = queryResponse.lastEvaluatedKey();
            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        } catch (DynamoDbException e) {
            logger.error("Error querying locale table {} for languageCode {}: {}", localeTable, languageCode, e.getMessage(), e);
            throw e;
        }

        return results;
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataByPageName(String pageName) {
        logger.info("Fetching locale data for pageName={} from table: {}", pageName, localeTable);
        List<LocaleRequestDTO> results = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        try {
            do {
                ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(localeTable)
                    .filterExpression("pageName = :pn")
                    .expressionAttributeValues(Map.of(":pn", AttributeValue.builder().s(pageName).build()))
                    .exclusiveStartKey(lastEvaluatedKey)
                    .build();

                ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
                List<Map<String, AttributeValue>> items = scanResponse.items();
                if (items != null) {
                    for (Map<String, AttributeValue> item : items) {
                        results.add(mapToLocaleRequestDTO(item));
                    }
                }
                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        } catch (DynamoDbException e) {
            logger.error("Error scanning locale table {} for pageName {}: {}", localeTable, pageName, e.getMessage(), e);
            throw e;
        }

        return results;
    }

    @Override
    public void deleteLocaleData(String languageCode, String pageName) {
        logger.info("Deleting locale data for languageCode={}, pageName={} from table: {}", languageCode, pageName, localeTable);
        try {
            Map<String, AttributeValue> key = Map.of(
                "languageCode", AttributeValue.builder().s(languageCode).build(),
                "pageName", AttributeValue.builder().s(pageName).build()
            );

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                .tableName(localeTable)
                .key(key)
                .build();

            dynamoDbClient.deleteItem(deleteRequest);
            logger.debug("Deleted locale item languageCode={}, pageName={} from table {}", languageCode, pageName, localeTable);
        } catch (DynamoDbException e) {
            logger.error("Error deleting locale data from DynamoDB: {}", e.getMessage(), e);
            throw e;
        }
    }

    private LocaleRequestDTO mapToLocaleRequestDTO(Map<String, AttributeValue> item) {
        if (item == null) return null;
        String languageCode = item.containsKey("languageCode") ? item.get("languageCode").s() : null;
        String pageName = item.containsKey("pageName") ? item.get("pageName").s() : null;
        String localeData = item.containsKey("localeData") ? item.get("localeData").s() : null;
        return new LocaleRequestDTO(languageCode, pageName, localeData);
    }
}
