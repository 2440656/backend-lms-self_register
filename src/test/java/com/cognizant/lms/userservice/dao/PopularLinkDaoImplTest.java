package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.exception.DataBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PopularLinkDaoImplTest {
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<Tenant> table;
    private PopularLinkDaoImpl dao;
    private DynamoDbClient dynamoDbClient;

    @BeforeEach
    void setUp() {
        enhancedClient = mock(DynamoDbEnhancedClient.class);
        table = mock(DynamoDbTable.class);
        DynamoDBConfig config = mock(DynamoDBConfig.class);
        dynamoDbClient = mock(DynamoDbClient.class);
        when(config.getDynamoDBEnhancedClient()).thenReturn(enhancedClient);
        when(enhancedClient.table(any(), any(TableSchema.class))).thenReturn(table);
        dao = new PopularLinkDaoImpl(config, "table", dynamoDbClient);
    }

    @Test
    void savePopularLink_success() {
        Tenant tenant = new Tenant();
        dao.savePopularLink(tenant);
        verify(table).putItem(tenant);
    }

    @Test
    void savePopularLink_dynamoDbException() {
        Tenant tenant = new Tenant();
        doThrow(DynamoDbException.builder().message("fail").build()).when(table).putItem(tenant);
        assertThrows(DataBaseException.class, () -> dao.savePopularLink(tenant));
    }

    @Test
    void savePopularLink_genericException() {
        Tenant tenant = new Tenant();
        doThrow(new RuntimeException("fail")).when(table).putItem(tenant);
        assertThrows(DataBaseException.class, () -> dao.savePopularLink(tenant));
    }

    @Test
    void updatePopularLink_missingLinkId() {
        Tenant tenant = new Tenant();
        tenant.setLinkId("");
        assertThrows(IllegalArgumentException.class, () -> dao.updatePopularLink(tenant));
        tenant.setLinkId(null);
        assertThrows(IllegalArgumentException.class, () -> dao.updatePopularLink(tenant));
    }

    @Test
    void updatePopularLink_dynamoDbException() {
        Tenant tenant = new Tenant();
        tenant.setPk("pk");
        tenant.setLinkId("id1");
        doThrow(DynamoDbException.builder().message("fail").build()).when(table).putItem(tenant);
        assertThrows(DataBaseException.class, () -> dao.updatePopularLink(tenant));
    }

    @Test
    void updatePopularLink_genericException() {
        Tenant tenant = new Tenant();
        tenant.setLinkId("id1");
        tenant.setPk("pk");
        doThrow(new RuntimeException("fail")).when(table).putItem(tenant);
        assertThrows(DataBaseException.class, () -> dao.updatePopularLink(tenant));
    }

    @Test
    void deletePopularLink_missingPkOrSk() {
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink("", "sk"));
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink(null, "sk"));
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink("pk", ""));
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink("pk", null));
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink("", ""));
        assertThrows(IllegalArgumentException.class, () -> dao.deletePopularLink(null, null));
    }

    @Test
    void deletePopularLink_success() {
        String pk = "somePk";
        String sk = "someSk";
        dao.deletePopularLink(pk, sk);
        verify(table).deleteItem(any(java.util.function.Consumer.class));
    }

    @Test
    void deletePopularLink_dynamoDbException() {
        String pk = "somePk";
        String sk = "someSk";
        doThrow(DynamoDbException.builder().message("fail").build()).when(table).deleteItem(any(java.util.function.Consumer.class));
        assertThrows(DataBaseException.class, () -> dao.deletePopularLink(pk, sk));
    }

    @Test
    void deletePopularLink_genericException() {
        String pk = "somePk";
        String sk = "someSk";
        doThrow(new RuntimeException("fail")).when(table).deleteItem(any(java.util.function.Consumer.class));
        assertThrows(DataBaseException.class, () -> dao.deletePopularLink(pk, sk));
    }

    @Test
    void getPopularLinkByPk_success() {
        String pk = "pk";
        String sk = "sk";
        Tenant tenant = new Tenant();
        when(table.getItem(isA(java.util.function.Consumer.class))).thenReturn(tenant);
        Tenant result = dao.getPopularLinkByPk(pk, sk);
        assertSame(tenant, result);
    }

    @Test
    void getPopularLinkByPk_exception() {
        String pk = "pk";
        String sk = "sk";
        when(table.getItem(isA(java.util.function.Consumer.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(DataBaseException.class, () -> dao.getPopularLinkByPk(pk, sk));
    }

    @Test
    void updatePopularLinksTransactional_success() {
        Tenant tenant1 = new Tenant();
        tenant1.setPk("pk1");
        tenant1.setLinkId("id1");
        Tenant tenant2 = new Tenant();
        tenant2.setPk("pk2");
        tenant2.setLinkId("id2");
        List<Tenant> tenants = List.of(tenant1, tenant2);
        TableSchema<Tenant> schema = mock(TableSchema.class);
        when(table.tableSchema()).thenReturn(schema);
        when(schema.itemToMap(any(), anyBoolean())).thenReturn(new java.util.HashMap<>());
        dao.updatePopularLinksTransactional(tenants);
        verify(dynamoDbClient).transactWriteItems(any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest.class));
    }

    @Test
    void updatePopularLinksTransactional_dynamoDbException() {
        Tenant tenant = new Tenant();
        tenant.setPk("pk1");
        tenant.setLinkId("id1");
        List<Tenant> tenants = List.of(tenant);
        TableSchema<Tenant> schema = mock(TableSchema.class);
        when(table.tableSchema()).thenReturn(schema);
        when(schema.itemToMap(any(), anyBoolean())).thenReturn(new java.util.HashMap<>());
        doThrow(DynamoDbException.builder().message("fail").build())
                .when(dynamoDbClient).transactWriteItems(any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest.class));
        assertThrows(DataBaseException.class, () -> dao.updatePopularLinksTransactional(tenants));
    }

    @Test
    void updatePopularLinksTransactional_genericException() {
        Tenant tenant = new Tenant();
        tenant.setPk("pk1");
        tenant.setLinkId("id1");
        List<Tenant> tenants = List.of(tenant);
        TableSchema<Tenant> schema = mock(TableSchema.class);
        when(table.tableSchema()).thenReturn(schema);
        when(schema.itemToMap(any(), anyBoolean())).thenReturn(new java.util.HashMap<>());
        doThrow(new RuntimeException("fail")).when(dynamoDbClient).transactWriteItems(any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest.class));
        assertThrows(DataBaseException.class, () -> dao.updatePopularLinksTransactional(tenants));
    }

    @Test
    void getPopularLinksByTenant_returnsSortedList() {
        String tenantCode = "tenant1";
        Tenant tenant1 = new Tenant(); tenant1.setName("B"); tenant1.setPk(tenantCode); tenant1.setSk("POPULARLINK#1"); tenant1.setIndex(2);
        Tenant tenant2 = new Tenant(); tenant2.setName("A"); tenant2.setPk(tenantCode); tenant2.setSk("POPULARLINK#2"); tenant2.setIndex(1);
        Tenant tenant3 = new Tenant(); tenant3.setName(null); tenant3.setPk(tenantCode); tenant3.setSk("POPULARLINK#3"); tenant3.setIndex(3);
        List<Tenant> tenants = List.of(tenant1, tenant2, tenant3);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(tenants);
        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(pageIterable.iterator()).thenReturn(List.of(page).iterator());
        when(pageIterable.stream()).thenReturn(List.of(page).stream());
        var index = mock(software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex.class);
        when(table.index("gsi_type")).thenReturn(index);
        when(index.query(any(QueryConditional.class))).thenReturn(pageIterable);

        List<Tenant> result = dao.getPopularLinksByTenant(tenantCode);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(tenant2, tenant1, tenant3), result);
    }

    @Test
    void getPopularLinksByTenant_returnsEmptyList() {
        String tenantCode = "tenant2";
        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(List.of());
        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(pageIterable.iterator()).thenReturn(List.of(page).iterator());
        when(pageIterable.stream()).thenReturn(List.of(page).stream());
        var index = mock(software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex.class);
        when(table.index("gsi_type")).thenReturn(index);
        when(index.query(any(QueryConditional.class))).thenReturn(pageIterable);

        List<Tenant> result = dao.getPopularLinksByTenant(tenantCode);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularLinksByTenant_dynamoDbException() {
        when(table.query(any(QueryConditional.class)))
                .thenThrow(DynamoDbException.builder().message("fail").build());
        assertThrows(DataBaseException.class, () -> dao.getPopularLinksByTenant("tenant3"));
    }
}