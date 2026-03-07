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
public class QuickLinkDaoImpl implements QuickLinkDao {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final String tenantTable;
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public QuickLinkDaoImpl(DynamoDBConfig dynamoDBConfig,
                            @Value("${AWS_DYNAMODB_TENANT_TABLE_NAME}") String tenantTable,
                            DynamoDbClient dynamoDbClient) {
        this.dynamoDbEnhancedClient = dynamoDBConfig.getDynamoDBEnhancedClient();
        this.tenantTable = tenantTable;
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void saveQuickLink(Tenant tenantQuickLink) {
        try {
            tenantQuickLink.setType("QUICKLINK");
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.putItem(tenantQuickLink);
            log.info("QuickLink saved successfully: {}", tenantQuickLink);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error saving QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error saving QuickLink to DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error saving QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error saving QuickLink: " + e.getMessage());
        }
    }

    @Override
    public Tenant updateQuickLink(Tenant tenantQuickLink) {
        if (tenantQuickLink.getLinkId() == null || tenantQuickLink.getLinkId().isEmpty()) {
            throw new IllegalArgumentException("linkId must be provided for update");
        }
        tenantQuickLink.setSk("QUICKLINK#" + tenantQuickLink.getLinkId());
        try {
            tenantQuickLink.setType("QUICKLINK");
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.putItem(tenantQuickLink);
            log.info("QuickLink updated successfully: {}", tenantQuickLink);
            return tenantQuickLink;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error updating QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error updating QuickLink in DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error updating QuickLink: " + e.getMessage());
        }
    }

    @Override
    public void deleteQuickLink(String pk, String sk) {
        if (pk == null || pk.isEmpty() || sk == null || sk.isEmpty()) {
            throw new IllegalArgumentException("pk and sk must be provided for delete");
        }
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            table.deleteItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
            log.info("QuickLink deleted successfully: {}|{}", pk, sk);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error deleting QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Error deleting QuickLink from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting QuickLink: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error deleting QuickLink: " + e.getMessage());
        }
    }

    @Override
    public Tenant getQuickLinkByPk(String pk, String sk) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            return table.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
        } catch (Exception e) {
            log.error("Error fetching QuickLink by pk and sk: {}", e.getMessage(), e);
            throw new DataBaseException("Error fetching QuickLink by pk and sk: " + e.getMessage());
        }
    }

    @Override
    public void updateQuickLinksTransactional(List<Tenant> links) {
        try {
            List<TransactWriteItem> transactItems = new ArrayList<>();
            for (Tenant tenantQuickLink : links) {
                tenantQuickLink.setType("QUICKLINK");
                tenantQuickLink.setSk("QUICKLINK#" + tenantQuickLink.getLinkId());
                DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
                var itemMap = table.tableSchema().itemToMap(tenantQuickLink, true);
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
            log.info("Transactionally updated {} QuickLinks", links.size());
        } catch (DynamoDbException e) {
            log.error("DynamoDB transaction error: {}", e.getMessage(), e);
            throw new DataBaseException("Error in transactional update: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in transactional update: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error in transactional update: " + e.getMessage());
        }
    }

    @Override
    public List<Tenant> getQuickLinksByTenant(String tenantCode) {
        try {
            DynamoDbTable<Tenant> table = dynamoDbEnhancedClient.table(tenantTable, TableSchema.fromBean(Tenant.class));
            var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue("QUICKLINK").build());
            List<Tenant> results = new ArrayList<>();
            table.index("gsi_type").query(queryConditional).stream()
                    .forEach(page -> {
                        for (Tenant t : page.items()) {
                            if (tenantCode.equals(t.getPk()) && t.getSk() != null && t.getSk().startsWith("QUICKLINK#")) {
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
            log.error("DynamoDB error fetching QuickLinks by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Error fetching QuickLinks by tenant from DynamoDB: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching QuickLinks by tenant: {}", e.getMessage(), e);
            throw new DataBaseException("Unexpected error fetching QuickLinks by tenant: " + e.getMessage());
        }
    }
}
