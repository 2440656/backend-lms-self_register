package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class MessageDaoImpl implements MessageDao {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final DynamoDbTable<Tenant> tenantTable;

    public MessageDaoImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient,
                          DynamoDBConfig dynamoDBConfig,
                          @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tableName) {
        this.dynamoDbClient = dynamoDBConfig.dynamoDbClient();
        this.tableName = tableName;
        this.tenantTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(Tenant.class));
    }

    private Key buildKey(String tenantCode, String category) {
        String sk = tenantCode + Constants.HASH + category;
        return Key.builder()
                .partitionValue(tenantCode)
                .sortValue(sk)
                .build();
    }

    @Override
    public Tenant findTenantByKey(String tenantCode, String category) {
        return tenantTable.getItem(r -> r.key(buildKey(tenantCode, category)));
    }

    @Override
    public List<Tenant> findTenantByCategoryAndStatus(String tenantCode, String category, Boolean status) {
        List<Tenant> matchingTenants = new ArrayList<>();

        PageIterable<Tenant> results = tenantTable.query(r ->
                r.queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(tenantCode).build()))
        );

        for (Tenant tenant : results.items()) {
            boolean matchesCategory = (category == null || category.equals(tenant.getCategory()));
            boolean matchesStatus = (status == null || status.equals(tenant.getMessageStatus()));

            if (category == null && status == null) {
                if (tenant.getMessage() != null && tenant.getMessageStatus() != null) {
                    matchingTenants.add(tenant);
                }
            } else if (matchesCategory && matchesStatus) {
                matchingTenants.add(tenant);
            }
        }

        return matchingTenants;
    }


    @Override
    public void saveTenant(Tenant tenant) {
        tenantTable.putItem(tenant);
    }
}

