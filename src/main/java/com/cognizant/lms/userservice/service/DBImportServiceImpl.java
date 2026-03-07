package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.DynamoDBConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.TableDisplayNameDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Service
@Slf4j
public class DBImportServiceImpl implements DBImportService {

  private final String rolesTable;
  private final String lookupTable;
  private final String skillLookupsTable;
  private String TABLE_NAME;
  private final DynamoDbClient dynamoDbClient;

  public DBImportServiceImpl(DynamoDBConfig dynamoDBConfig,
      @Value("${AWS_DYNAMODB_ROLES_TABLE_NAME}") String rolesTable,
      @Value("${AWS_DYNAMODB_LOOKUP_TABLE_NAME}") String lookupTable,
      @Value("${AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME}") String skillLookupsTable) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.rolesTable = rolesTable;
    this.lookupTable = lookupTable;
    this.skillLookupsTable = skillLookupsTable;
  }

  public Map<String, String> getTableDisplayMap() {
    Map<String, String> map = new HashMap<>();
    if (rolesTable != null && !rolesTable.isEmpty()) map.put("Roles Master Table", rolesTable);
    if (lookupTable != null && !lookupTable.isEmpty()) map.put("Lookup Master Table", lookupTable);
    if (skillLookupsTable != null && !skillLookupsTable.isEmpty()) map.put("Skill Lookups Master Table", skillLookupsTable);
    return map;
  }

  public String getActualTableName(String displayName) {
    String tableName = getTableDisplayMap().get(displayName);
    if (tableName == null) {
      throw new IllegalArgumentException("No table mapping found for display name: " + displayName);
    }
    return tableName;
  }

  public List<TableDisplayNameDto> getTableDisplayNames() {
    List<TableDisplayNameDto> displayNames = new ArrayList<>();
    Map<String, String> tableMap = getTableDisplayMap();
    for (Map.Entry<String, String> entry : tableMap.entrySet()) {
      String tableType =  "master";
      displayNames.add(new TableDisplayNameDto(tableType, entry.getKey()));
    }
    return displayNames;
  }

  @Override
  public int dbImportdata(MultipartFile file, String tableDisplayName) {
    String tableName = getActualTableName(tableDisplayName);
    this.TABLE_NAME = tableName;
    int countOfImport = 0;
    int totalRecords = 0;
    log.info("DynamoDB table {}", tableName);
    log.info("DynamoDB file {}", file.getOriginalFilename());
    try (InputStream inputStream = file.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
      totalRecords = (int) csvParser.getRecords().size();
      log.info("Total records in CSV: {}", totalRecords);
    } catch (Exception e) {
      log.error("Error counting records in CSV: {}", e.getMessage());
    }
    try {
      countOfImport = importCsvToDynamoDb(dynamoDbClient, file);
      log.info("Imported {} records to DynamoDB", countOfImport);
    } catch (Exception e) {
      log.error("Error importing data to DynamoDB: {}", e.getMessage());
    }
    return countOfImport;
  }

  private int importCsvToDynamoDb(DynamoDbClient dynamoDbClient, MultipartFile file) {
    int recordCount = 0;
    log.info("File OriginalFilename {}", file.getOriginalFilename());
    try (InputStream inputStream = file.getInputStream();
         BufferedReader reader = new BufferedReader(new
             InputStreamReader(inputStream, StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
      List<WriteRequest> writeRequests = new ArrayList<>();
      Set<String> uniqueKeys = new HashSet<>();
      for (CSVRecord csvRecord : csvParser) {
        Map<String, AttributeValue> item = new HashMap<>();
        String primaryKey = csvRecord.get("pk");
        String sortKey = csvRecord.get("sk");
        String combinedKey = primaryKey + "#" + sortKey;
        if (uniqueKeys.contains(combinedKey)) {
          log.warn("Duplicate key combination found: {}", combinedKey);
          continue;
        }
        uniqueKeys.add(combinedKey);

        csvRecord.toMap().forEach((key, value) -> item.put(key,
            AttributeValue.builder().s(value).build()));

        writeRequests.add(WriteRequest.builder()
            .putRequest(PutRequest.builder().item(item).build())
            .build());

        if (writeRequests.size() == 25) {
          recordCount+=batchWriteWithRetry(dynamoDbClient, TABLE_NAME, writeRequests);
          writeRequests.clear();
        }
      }

      if (!writeRequests.isEmpty()) {
        recordCount+=batchWriteWithRetry(dynamoDbClient, TABLE_NAME, writeRequests);
      }
      log.info("Imported {} records to DynamoDB", recordCount);
    } catch (IOException e) {
      log.error("Error reading CSV file: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Error writing to DynamoDB: {}", e.getMessage());
    }
    log.info("Completed batch upload for table {}. Total records imported: {}", TABLE_NAME, recordCount);
    return recordCount;

  }

  private int batchWriteWithRetry(DynamoDbClient dynamoDbClient, String tableName, List<WriteRequest> writeRequests) {
    Map<String, List<WriteRequest>> requestItems = new HashMap<>();
    requestItems.put(tableName, new ArrayList<>(writeRequests));
    int attempt = 1;
    int maxRetries = 5;
    int successfulWrites = writeRequests.size();

    log.debug("Starting batch upload for table {} with {} records", tableName, writeRequests.size());
    while (!requestItems.isEmpty() && attempt <= maxRetries) {
      log.debug("Batch write attempt {} for table {} with {} items", attempt, tableName, requestItems.get(tableName).size());

      BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(
              BatchWriteItemRequest.builder().requestItems(requestItems).build()
      );
      requestItems = response.unprocessedItems();
      if (!requestItems.isEmpty()) {
        successfulWrites -= requestItems.getOrDefault(tableName,List.of()).size();
        log.warn("Unprocessed items found in batch write, retrying...");
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
      }
      attempt++;
    }

    if (!requestItems.isEmpty()) {
      log.error("Permanent failure: Unprocessed items after {} retries for table {}: {}", maxRetries, tableName, requestItems);
    }

    return successfulWrites;
  }

  @Override
  public String getTableDataAsCSV(String tableDisplayName) throws IOException {
    String tableName = getActualTableName(tableDisplayName);
    StringWriter out = new StringWriter();
    Map<String, AttributeValue> lastEvaluatedKey = null;
    List<Map<String, AttributeValue>> allItems = new ArrayList<>();
    Set<String> headers = new LinkedHashSet<>();

    do {
      ScanRequest scanRequest = ScanRequest.builder()
          .tableName(tableName)
          .exclusiveStartKey(lastEvaluatedKey)
          .build();

      ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
      List<Map<String, AttributeValue>> items = scanResponse.items();

      if (!items.isEmpty()) {
        allItems.addAll(items);
        for (Map<String, AttributeValue> item : items) {
          headers.addAll(item.keySet());
        }
      }

      lastEvaluatedKey = scanResponse.lastEvaluatedKey();
    } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

    if (allItems.isEmpty()) {
      return out.toString();
    }

    try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))) {
      for (Map<String, AttributeValue> item : allItems) {
        List<String> record = new ArrayList<>();
        for (String header : headers) {
          record.add(attributeValueToCsvValue(item.get(header)));
        }
        printer.printRecord(record);
      }
    }

    return out.toString();
  }

  private String attributeValueToCsvValue(AttributeValue attributeValue) {
    if (attributeValue == null || Boolean.TRUE.equals(attributeValue.nul())) {
      return "";
    }
    if (attributeValue.s() != null) {
      return attributeValue.s();
    }
    if (attributeValue.n() != null) {
      return attributeValue.n();
    }
    if (attributeValue.bool() != null) {
      return String.valueOf(attributeValue.bool());
    }
    if (attributeValue.ss() != null && !attributeValue.ss().isEmpty()) {
      return String.join("|", attributeValue.ss());
    }
    if (attributeValue.ns() != null && !attributeValue.ns().isEmpty()) {
      return String.join("|", attributeValue.ns());
    }
    if (attributeValue.bs() != null && !attributeValue.bs().isEmpty()) {
      return attributeValue.bs().stream()
          .map(bytes -> Base64.getEncoder().encodeToString(bytes.asByteArray()))
          .collect(Collectors.joining("|"));
    }
    if (attributeValue.l() != null && !attributeValue.l().isEmpty()) {
      return attributeValue.l().stream()
          .map(this::attributeValueToCsvValue)
          .collect(Collectors.joining(",", "[", "]"));
    }
    if (attributeValue.m() != null && !attributeValue.m().isEmpty()) {
      return attributeValue.m().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(entry -> entry.getKey() + ":" + attributeValueToCsvValue(entry.getValue()))
          .collect(Collectors.joining(",", "{", "}"));
    }
    return "";
  }

  @Override
  public boolean clearTableData(String tableDisplayName) throws Exception {
    String tableName = getActualTableName(tableDisplayName);
    try {
      Map<String, AttributeValue> lastEvaluatedKey = null;

      do {
        ScanRequest scanRequest = ScanRequest.builder()
            .tableName(tableName)
            .exclusiveStartKey(lastEvaluatedKey)
            .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
        List<Map<String, AttributeValue>> items = scanResponse.items();

        List<WriteRequest> deleteRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : items) {
          Map<String, AttributeValue> key = new HashMap<>();
          key.put("pk", item.get("pk"));
          key.put("sk", item.get("sk"));

          deleteRequests.add(WriteRequest.builder()
              .deleteRequest(DeleteRequest.builder().key(key).build())
              .build());

          if (deleteRequests.size() == 25) {
            BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                .requestItems(Map.of(tableName, deleteRequests))
                .build();
            dynamoDbClient.batchWriteItem(batchWriteItemRequest);
            deleteRequests.clear();
          }
        }

        if (!deleteRequests.isEmpty()) {
          BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
              .requestItems(Map.of(tableName, deleteRequests))
              .build();
          dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        }

        lastEvaluatedKey = scanResponse.lastEvaluatedKey();
      } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

      return true;
    } catch (Exception e) {
      log.error("Error emptying table in DynamoDB: {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public int updateUserCountry(String tableDisplayName) {
    String tableName = getActualTableName(tableDisplayName);
    AtomicInteger totalUpdatedRecords = new AtomicInteger();

    try {
      Map<String, AttributeValue> lastEvaluatedKey = null;

      do {
        // Scan for users without a country field
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("attribute_not_exists(#country) OR #country = :emptyCountry")
                .expressionAttributeNames(Map.of("#country", "country"))
                .expressionAttributeValues(Map.of(":emptyCountry", AttributeValue.builder().s("").build()))
                .exclusiveStartKey(lastEvaluatedKey)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        if (scanResponse == null) {
          log.error("ScanResponse is null for table: {}", tableName);
          throw new RuntimeException("Failed to scan DynamoDB table.");
        }
        List<Map<String, AttributeValue>> items = scanResponse.items();

        List<WriteRequest> updateRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : items) {
          Map<String, AttributeValue> key = Map.of(
                  "pk", item.get("pk"),
                  "sk", item.get("sk")
          );

          Map<String, AttributeValue> updatedItem = new HashMap<>(item);
          updatedItem.put("country", AttributeValue.builder().s(Constants.DEFAULT_COUNTRY).build());

          updateRequests.add(WriteRequest.builder()
                  .putRequest(PutRequest.builder().item(updatedItem).build())
                  .build());

          if (updateRequests.size() == 25) {
            BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                    .requestItems(Map.of(tableName, updateRequests))
                    .build();
            BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
            totalUpdatedRecords.addAndGet(updateRequests.size() - response.unprocessedItems().size());
            updateRequests.clear();
          }
        }

        if (!updateRequests.isEmpty()) {
          BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                  .requestItems(Map.of(tableName, updateRequests))
                  .build();
          BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
          totalUpdatedRecords.addAndGet(updateRequests.size() - response.unprocessedItems().size());
        }

        lastEvaluatedKey = scanResponse.lastEvaluatedKey();
      } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

    } catch (Exception e) {
      log.error("Error updating user country field: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    return totalUpdatedRecords.get();
  }

  public String deleteTableDataByPkSkAndTableName(String tableDisplayName, MultipartFile file) {
    String tableName;
    try {
      tableName = getActualTableName(tableDisplayName);
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
    if (!"text/csv".equals(file.getContentType()) || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
      return "Error: File must be a CSV format.";
    }

    try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
         CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
             .withFirstRecordAsHeader()
             .withIgnoreHeaderCase()
             .withTrim())) {

      List<Map<String, AttributeValue>> keyMaps = new ArrayList<>();

      final String PK_COL = "pk";
      final String SK_COL = "sk";

      for (CSVRecord csvRecord : csvParser) {
        Map<String, AttributeValue> key = new HashMap<>();

        key.put(PK_COL, AttributeValue.builder().s(csvRecord.get(PK_COL)).build());
        key.put(SK_COL, AttributeValue.builder().s(csvRecord.get(SK_COL)).build());

        keyMaps.add(key);

        if (keyMaps.size() == 25) {
          processBatchDeleteByPkSkAndTableName(tableName, keyMaps);
          keyMaps.clear();
        }
      }

      if (!keyMaps.isEmpty()) {
        processBatchDeleteByPkSkAndTableName(tableName, keyMaps);
      }

      return "Successfully initiated batch deletion for all records.";
    } catch (Exception e) {
      return "Error during deletion: " + e.getMessage();
    }
  }


  public void processBatchDeleteByPkSkAndTableName(String tableName, List<Map<String, AttributeValue>> keyMaps) {
    List<Map<String, AttributeValue>> allKeysToProcess = new ArrayList<>(keyMaps);
    final int MAX_ATTEMPTS = 5;
    final int BATCH_SIZE = 25;

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      if (allKeysToProcess.isEmpty()) {
        break;
      }

      List<Map<String, AttributeValue>> keysForNextAttempt = new ArrayList<>();

      for (int i = 0; i < allKeysToProcess.size(); i += BATCH_SIZE) {
        int endIndex = Math.min(i + BATCH_SIZE, allKeysToProcess.size());
        List<Map<String, AttributeValue>> currentBatchKeys = allKeysToProcess.subList(i, endIndex);

        List<WriteRequest> writeRequests = new ArrayList<>();
        for (Map<String, AttributeValue> key : currentBatchKeys) {
          DeleteRequest deleteRequest = DeleteRequest.builder().key(key).build();
          WriteRequest writeRequest = WriteRequest.builder().deleteRequest(deleteRequest).build();
          writeRequests.add(writeRequest);
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(tableName, writeRequests);

        BatchWriteItemRequest batchRequest = BatchWriteItemRequest.builder()
            .requestItems(requestItems)
            .build();

        try {
          BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchRequest);

          if (response.hasUnprocessedItems() && response.unprocessedItems().containsKey(tableName)) {
            log.info("Attempt {}: Found {} unprocessed items in a batch for table {}. Will retry.",
                attempt, response.unprocessedItems().get(tableName).size(), tableName);

            for (WriteRequest unprocessedWriteRequest : response.unprocessedItems().get(tableName)) {
              keysForNextAttempt.add(unprocessedWriteRequest.deleteRequest().key());
            }
          }
        } catch (DynamoDbException e) {
          log.info("DynamoDB BatchWriteItem failed for table {} during attempt {}: {}",
              tableName, attempt, e.getMessage());
          keysForNextAttempt.addAll(currentBatchKeys);
        } catch (Exception e) {
          log.info("BatchWriteItem failed unexpectedly for table {} during attempt {}: {}",
              tableName, attempt, e.getMessage());
          keysForNextAttempt.addAll(currentBatchKeys);
        }
      }

      allKeysToProcess.clear();
      allKeysToProcess.addAll(keysForNextAttempt);

      if (!allKeysToProcess.isEmpty() && attempt < MAX_ATTEMPTS) {
        long delay = (long) Math.pow(2, attempt) * 100;
        log.info("Waiting for {}ms before attempt {}...", delay, attempt + 1);
        try {
          TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.info("Thread interrupted during retry delay. Aborting processing.");
          allKeysToProcess.clear();
          break;
        }
      }
    }

    if (!allKeysToProcess.isEmpty()) {
      log.info("FATAL: Failed to process {} items after " + MAX_ATTEMPTS + " attempts.", allKeysToProcess.size());
    }
  }
}