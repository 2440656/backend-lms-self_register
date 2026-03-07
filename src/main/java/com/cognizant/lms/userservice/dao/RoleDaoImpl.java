package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.utils.LogUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@Repository
@Slf4j
public class RoleDaoImpl implements RoleDao {

  private DynamoDbClient dynamoDbClient;
  private String tableName;
  private String partitionKeyName;

  public RoleDaoImpl(DynamoDBConfig dynamoDBConfig,
                     @Value("${AWS_DYNAMODB_ROLES_TABLE_NAME}") String tableName,
                     @Value("${AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME}")
                     String partitionKeyName) {
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
    this.partitionKeyName = partitionKeyName;
  }

  @Override
  public List<RoleDto> getRoles() {
    log.info(LogUtil.getLogInfo(ProcessConstants.GET_ROLES,
        ProcessConstants.IN_PROGRESS) + "Fetching all roles");
    List<RoleDto> roleDtoList = new ArrayList<>();
    String partitionKeyValue = "Role";
    try {
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression(partitionKeyName + " = :PK")
          .consistentRead(false)
          .scanIndexForward(true)
          .expressionAttributeValues(
              Map.of(
                  ":PK", AttributeValue.builder().s(partitionKeyValue).build()
              ))
          .build();
      QueryResponse queryResult = dynamoDbClient.query(queryRequest);

      roleDtoList = queryResult.items().stream()
          .map(this::mapItemToRoleDto)
          .distinct()
          .collect(Collectors.toList());
    } catch (DynamoDbException e) {
      log.error(LogUtil.getLogError(ProcessConstants.GET_ROLES,
          HttpStatus.INTERNAL_SERVER_ERROR.toString(),
          ProcessConstants.FAILED)
          + "Error reading roles from DynamoDB {} : ", e.getMessage(), e.getStackTrace());
    }
    return roleDtoList;
  }

  private RoleDto mapItemToRoleDto(Map<String, AttributeValue> item) {
    RoleDto roleDto = new RoleDto();
    for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
      try {
        if (entry.getKey().equals("pk") || entry.getKey().equals("sk")
            || entry.getKey().equals("name")) {
          Field field = RoleDto.class.getDeclaredField(entry.getKey());
          field.setAccessible(true);
          field.set(roleDto, entry.getValue() != null ? entry.getValue().s() : null);
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.info("Error occurred while mapping roleDto attributes {} : ",
            e.getMessage());
      }
    }
    return roleDto;
  }
}
