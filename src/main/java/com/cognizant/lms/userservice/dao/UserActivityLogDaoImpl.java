package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.UserActivityLogDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class UserActivityLogDaoImpl implements UserActivityLogDao {
    private final DynamoDBConfig dynamoDBConfig;
    private final String userActivityLogTableName;

    public UserActivityLogDaoImpl(DynamoDBConfig dynamoDBConfig,
                                  @Value("${AWS_DYNAMODB_USER_ACTIVITY_LOG_TABLE_NAME}") String userActivityLogTableName) {
        this.dynamoDBConfig = dynamoDBConfig;
        this.userActivityLogTableName = userActivityLogTableName;
    }

    @Override
    public void saveUserActivityLog(UserActivityLogDto userActivityLog) {
        try {
            DynamoDbTable<UserActivityLogDto> activityLogTable = dynamoDBConfig.getDynamoDBEnhancedClient()
                    .table(userActivityLogTableName, TableSchema.fromBean(UserActivityLogDto.class));
            activityLogTable.putItem(userActivityLog);
            log.info("User {} activity logged successfully for user: {} {}", userActivityLog.getActivityType(), userActivityLog.getFirstName(), userActivityLog.getLastName());
        } catch (DynamoDbException e) {
            log.error("Error logging user logout activity: {}", e.getMessage());
            throw new RuntimeException("Failed to log user activity in DynamoDB", e);
        }
    }

    @Override
    public List<UserActivityLogDto> findByTimestamp(String pk, String timestamp) {
        try {
            DynamoDbTable<UserActivityLogDto> activityLogTable = dynamoDBConfig.getDynamoDBEnhancedClient()
                    .table(userActivityLogTableName, TableSchema.fromBean(UserActivityLogDto.class));
            var queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(pk)
                            .sortValue(timestamp)
                            .build());
            var results = activityLogTable.index("gsi_token_iat").query(queryConditional);
            List<UserActivityLogDto> logs = new ArrayList<>();
            results.stream().forEach(page -> logs.addAll(page.items()));
            return logs;
        } catch (DynamoDbException e) {
            log.error("Error querying user activity by pk and timestamp: {}", e.getMessage());
            throw new RuntimeException("Failed to query user activity by pk and timestamp in DynamoDB", e);
        }
    }
}
