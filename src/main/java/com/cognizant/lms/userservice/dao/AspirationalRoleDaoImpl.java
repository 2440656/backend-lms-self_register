package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.AspirationalRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class AspirationalRoleDaoImpl implements AspirationalRoleDao {

    private final DynamoDbTable<AspirationalRole> aspirationalRoleTable;

    public AspirationalRoleDaoImpl(DynamoDBConfig dynamoDBConfig,
                                   @Value("${AWS_DYNAMODB_ASPIRATIONAL_ROLE_TABLE_NAME}") String tableName) {
        this.aspirationalRoleTable = dynamoDBConfig.getDynamoDBEnhancedClient()
                .table(tableName, TableSchema.fromBean(AspirationalRole.class));
    }

    @Override
    public List<AspirationalRole> getAllAspirationalRoles() {
        try {
            log.info("Fetching all aspirational roles from DynamoDB table: {}", aspirationalRoleTable.tableName());
            List<AspirationalRole> allRoles = new ArrayList<>();
            
            // Scan all items in the table
            aspirationalRoleTable.scan().items().forEach(role -> {
                log.debug("Scanned item: PK={}, SK={}, userRoleName='{}', interestName='{}', roleName='{}'", 
                         role.getPk(), role.getSk(), 
                         role.getUserRoleName(), role.getInterestName(), role.getRoleName());
                allRoles.add(role);
            });
            
            log.info("Successfully fetched {} aspirational roles from DynamoDB", allRoles.size());
            return allRoles;
        } catch (Exception e) {
            log.error("Error fetching all aspirational roles: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch aspirational roles", e);
        }
    }

    @Override
    public List<AspirationalRole> getAspirationalRolesByType(String type) {
        try {
            log.info("Fetching aspirational roles by type: {}", type);
            List<AspirationalRole> roles = new ArrayList<>();
            
            // Query by partition key
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(type).build()
            );
            
            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .build();
            
            aspirationalRoleTable.query(queryRequest).items().forEach(roles::add);
            
            log.info("Successfully fetched {} aspirational roles for type: {}", roles.size(), type);
            return roles;
        } catch (Exception e) {
            log.error("Error fetching aspirational roles by type {}: {}", type, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch aspirational roles by type", e);
        }
    }
}
