package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.UserAuditLogDto;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
public class UserAuditLogDaoImpl implements UserAuditLogDao {
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTable<UserAuditLogDto> userAuditLogTable;
    private final String reminderLogTableName = System.getenv("AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME");

    public UserAuditLogDaoImpl(DynamoDBConfig dynamoDBConfig) {
        dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.userAuditLogTable = dynamoDBConfig.dynamoDbEnhancedClient(dynamoDbClient)
                .table(reminderLogTableName, TableSchema.fromBean(UserAuditLogDto.class));
    }

    @Override
    public UserAuditLogDto addUserAuditLog(UserAuditLogDto auditLogDto) {
        log.info("Inserting user audit log for user id: {}", auditLogDto.getEmailId());
        userAuditLogTable.putItem(auditLogDto);
        log.info("User audit log inserted successfully for user id: {}", auditLogDto.getEmailId());
        return auditLogDto;
    }

}
