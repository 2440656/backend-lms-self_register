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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class PopularLinkDaoImpl implements PopularLinkDao {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final String tenantTable;
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public PopularLinkDaoImpl(DynamoDBConfig dynamoDBConfig,
                              @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tenantTable,
                              DynamoDbClient dynamoDbClient) {
        this.dynamoDbEnhancedClient = dynamoDBConfig.getDynamoDBEnhancedClient();
        this.tenantTable = tenantTable;
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void savePopularLink(Tenant tenantPopularLink) {
        try {
            tenantPopularLink.setType("POPULARLINK");
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.putItem(tenantPopularLink);
            log.info("PopularLink saved successfully: {}", tenantPopularLink);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error saving PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error saving PopularLink to DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error saving PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error saving PopularLink: " + e.getMessage());
        }
    }

    @Override
    public Tenant updatePopularLink(Tenant tenantPopularLink) {
        if (tenantPopularLink.getLinkId() == null || tenantPopularLink.getLinkId().isEmpty()) {
            throw new IllegalArgumentException("linkId must be provided for update");
        }
        // Ensure sk is in the correct format
        tenantPopularLink.setSk("POPULARLINK#" + tenantPopularLink.getLinkId());
        try {
            tenantPopularLink.setType("POPULARLINK");
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.putItem(tenantPopularLink);
            log.info("PopularLink updated successfully: {}", tenantPopularLink);
            return tenantPopularLink;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error updating PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error updating PopularLink in DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error updating PopularLink: " + e.getMessage());
        }
    }

    @Override
    public void deletePopularLink(String pk, String sk) {
        if (pk == null || pk.isEmpty() || sk == null || sk.isEmpty()) {
            throw new IllegalArgumentException("pk and sk must be provided for delete");
        }
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.deleteItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
            log.info("PopularLink deleted successfully: {}|{}", pk, sk);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error deleting PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error deleting PopularLink from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting PopularLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error deleting PopularLink: " + e.getMessage());
        }
    }

    @Override
    public Tenant getPopularLinkByPk(String pk, String sk) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            return table.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
        } catch (Exception e) {
            log.error("Error fetching PopularLink by pk and sk: {}", e.getMessage(), e);
            throw new DataBaseException("Error fetching PopularLink by pk and sk: " + e.getMessage());
        }
    }

    /**
     * Transactionally update a list of PopularLinks (atomic reorder)
     */
    @Override
    public void updatePopularLinksTransactional(List<Tenant> links) {
        try {
            List<TransactWriteItem> transactItems = new ArrayList<>();
            for (Tenant tenantPopularLink : links) {
                tenantPopularLink.setType("POPULARLINK");
                // Ensure sk is in the correct format
                tenantPopularLink.setSk("POPULARLINK#" + tenantPopularLink.getLinkId());
                DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
                var itemMap = table.tableSchema().itemToMap(tenantPopularLink, true);
                Put put = Put.builder()
                        .tableName(tenantTable)
                        .item(itemMap)
                        .build();
                transactItems.add(TransactWriteItem.builder().put(put).build());
            }
            TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                    .transactItems(transactItems)
                    .build();
            dynamoDbClient.transactWriteItems(request);
            log.info("Transactionally updated {} PopularLinks", links.size());
        } catch (DynamoDbException e) {
            log.error("DynamoDB transaction error: {}", e.getMessage(), e);
            throw new DataBaseException("Error in transactional update: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in transactional update: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error in transactional update: " + e.getMessage());
        }
    }

    @Override
    public List<Tenant> getPopularLinksByTenant(String tenantCode) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue("POPULARLINK").build());
            List<Tenant> results = new ArrayList<>();
            table.index("gsi_type").query(queryConditional).stream()
                .forEach(page -> {
                    for (Tenant t : page.items()) {
                        if (tenantCode.equals(t.getPk()) && t.getSk() != null && t.getSk().startsWith("POPULARLINK#")) {
                            results.add(t);
                        }
                    }
                });
            results.sort((a, b) -> {
                if (a.getIndex() == null && b.getIndex() == null) return 0;
                if (a.getIndex() == null) return 1;
                if (b.getIndex() == null) return -1;
                return Integer.compare(a.getIndex(), b.getIndex());
            });
            return results;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error fetching PopularLinks by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Error fetching PopularLinks by tenant from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching PopularLinks by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error fetching PopularLinks by tenant: " + e.getMessage());
        }
    }
}
