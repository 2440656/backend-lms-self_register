package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.exception.DataBaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class BannerManagementDaoImpl implements BannerManagementDao {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final String tenantTable;
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public BannerManagementDaoImpl(DynamoDBConfig dynamoDBConfig,
                              @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tenantTable,
                              DynamoDbClient dynamoDbClient) {
        this.dynamoDbEnhancedClient = dynamoDBConfig.getDynamoDBEnhancedClient();
        this.tenantTable = tenantTable;
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void saveBannerManagementIcon(Tenant tenantBannerManagement) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.putItem(tenantBannerManagement);
            log.info("Banner saved successfully: {}", tenantBannerManagement);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error saving BannerManagement: {}", e.getMessage(), e);
            throw new DataBaseException("Error saving BannerManagement to DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error saving BannerManagement: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error saving BannerManagement: " + e.getMessage());
        }
    }

    @Override
    public int countActiveBanners(String tenantCode) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            return (int) table.scan().items().stream()
                .filter(tenant -> tenantCode.equals(tenant.getPk()) &&
                                 tenant.getSk() != null && tenant.getSk().startsWith("BANNER#") &&
                                 "ACTIVE".equals(tenant.getBannerStatus()))
                .count();
        } catch (DynamoDbException e) {
            log.error("DynamoDB error counting active banners: {}", e.getMessage(), e);
            throw new DataBaseException("Error counting active banners in DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error counting active banners: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error counting active banners: " + e.getMessage());
        }
    }

    @Override
    public Tenant getBannerById(String tenantCode, String bannerId) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            String cleanBannerId = bannerId.startsWith("BANNER#") ? bannerId.substring(7) : bannerId;
            String sk = "BANNER#" + cleanBannerId;
            software.amazon.awssdk.enhanced.dynamodb.Key key = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
                .partitionValue(tenantCode)
                .sortValue(sk)
                .build();
            return table.getItem(key);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error getting banner by ID: {}", e.getMessage(), e);
            throw new DataBaseException("Error getting banner from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting banner by ID: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error getting banner: " + e.getMessage());
        }
    }

    @Override
    public List<Tenant> getBannersByTenant(String tenantCode) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(tenantCode).build());
            List<Tenant> results = new ArrayList<>();
            table.query(r -> r.queryConditional(queryConditional)).stream()
                    .forEach(page -> {
                        for (Tenant t : page.items()) {
                            if (t != null && t.getSk() != null && t.getSk().startsWith("BANNER#")) {
                                results.add(t);
                            }
                        }
                    });

            // Sort by createdDate ascending (insertion order).
            results.sort(Comparator.nullsLast(Comparator.comparing(
                    t -> {
                        try {
                            return t.getCreatedOn() != null ? Instant.parse(t.getCreatedOn()) : Instant.EPOCH;
                        } catch (Exception ex) {
                            return Instant.EPOCH;
                        }
                    })
            ));

            return results;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error fetching Banners by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Error fetching Banners by tenant from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching Banners by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error fetching Banners by tenant: " + e.getMessage());
        }
    }

    @Override
    public void deleteBannerById(String tenantCode, String bannerId) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            String cleanBannerId = bannerId.startsWith("BANNER#") ? bannerId.substring(7) : bannerId;
            String sk = "BANNER#" + cleanBannerId;
            Key key = Key.builder()
                .partitionValue(tenantCode)
                .sortValue(sk)
                .build();
            table.deleteItem(key);
            log.info("Banner deleted successfully for bannerId: {}", bannerId);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error deleting banner: {}", e.getMessage(), e);
            throw new DataBaseException("Error deleting banner from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting banner: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error deleting banner: " + e.getMessage());
        }
    }
}
