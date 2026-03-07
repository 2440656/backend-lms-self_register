  package com.cognizant.lms.userservice.dao;

  import com.cognizant.lms.userservice.config.DynamoDBConfig;
  import com.cognizant.lms.userservice.domain.OperationsHistory;
  import com.cognizant.lms.userservice.exception.DataBaseException;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Repository;
  import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
  import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
  import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
  import software.amazon.awssdk.services.dynamodb.model.*;

  import java.util.Map;

  @Repository
  @Slf4j
  public class OperationsHistoryDaoImpl implements OperationsHistoryDao {
    private final DynamoDbTable<OperationsHistory> operationsHistoryTable;
//    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public OperationsHistoryDaoImpl(DynamoDBConfig dynamoDBConfig,
                                    @Value("${AWS_DYNAMODB_LOGFILE_TABLE_NAME}") String tableName) {
      this.operationsHistoryTable = dynamoDBConfig.getDynamoDBEnhancedClient()
          .table(tableName, TableSchema.fromBean(OperationsHistory.class));
//        this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    }

    @Override
    public void saveLogFileData(OperationsHistory logFile) {
      operationsHistoryTable.putItem(logFile);
    }

//    @Override
//    public boolean updateLogFileDataStatus(String pk, String sk, String errorLogfileName, String status, String updatedBy, String updatedOn) {
//      Map<String, AttributeValue> key = Map.of(
//          "pk", AttributeValue.builder().s(pk).build(),
//          "sk", AttributeValue.builder().s(sk).build()
//      );
//
//      try {
//        Map<String, AttributeValue> exprValues = Map.of(
//            ":fileName", AttributeValue.builder().s(errorLogfileName).build(),
//            ":status", AttributeValue.builder().s(status).build(),
//            ":updatedBy", AttributeValue.builder().s(updatedBy).build(),
//            ":updatedOn", AttributeValue.builder().s(updatedOn).build()
//        );
//
//        Map<String, String> exprNames = Map.of(
//            "#statusAlias", "status"
//        );
//        String updateExpr = "SET fileName = :fileName, #statusAlias = :status, " +
//            "updatedBy = :updatedBy, updatedOn = :updatedOn";
//
//        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
//            .tableName(operationsHistoryTable.tableName())
//            .key(key)
//            .updateExpression(updateExpr)
//            .expressionAttributeValues(exprValues)
//            .expressionAttributeNames(exprNames)
//            .conditionExpression("attribute_exists(pk) AND attribute_exists(sk)")
//            .build();
//
//
//        UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
//        return response.sdkHttpResponse().isSuccessful();
//
//      } catch (ConditionalCheckFailedException e) {
//        log.error("No existing log file entry found for PK: {}, SK: {}.", pk, sk);
//        return false;
//
//      } catch (DynamoDbException ex) {
//        log.error("Error writing to DynamoDB: {}", ex.getMessage());
//        throw new DataBaseException("DynamoDB operation failed: " + ex.getMessage());
//      }
//    }
  }