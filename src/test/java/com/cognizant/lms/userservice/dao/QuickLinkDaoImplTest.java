package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.exception.DataBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuickLinkDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDbTable<Tenant> tenantTable;

    @Mock
    private DynamoDbIndex<Tenant> gsiTypeIndex;

    private QuickLinkDaoImpl quickLinkDao;

    private static final String TENANT_TABLE_NAME = "tenant-table";
    private static final String TENANT_CODE = "TENANT001";
    private static final String LINK_ID = "link123";

    @BeforeEach
    void setUp() {
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(dynamoDbEnhancedClient);
        quickLinkDao = new QuickLinkDaoImpl(dynamoDBConfig, TENANT_TABLE_NAME, dynamoDbClient);
    }

    @Test
    void testSaveQuickLink_Success() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        // Act
        quickLinkDao.saveQuickLink(quickLink);

        // Assert
        verify(tenantTable).putItem(quickLink);
        assertEquals("QUICKLINK", quickLink.getType());
    }

    @Test
    void testSaveQuickLink_DynamoDbException() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        doThrow(DynamoDbException.builder().message("DynamoDB error").build())
                .when(tenantTable).putItem(any(Tenant.class));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.saveQuickLink(quickLink));
        assertTrue(exception.getMessage().contains("Error saving QuickLink to DynamoDB"));
    }

    @Test
    void testSaveQuickLink_UnexpectedException() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        doThrow(new RuntimeException("Unexpected error"))
                .when(tenantTable).putItem(any(Tenant.class));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.saveQuickLink(quickLink));
        assertTrue(exception.getMessage().contains("Unexpected error saving QuickLink"));
    }

    @Test
    void testUpdateQuickLink_Success() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        quickLink.setLinkId(LINK_ID);
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        // Act
        Tenant result = quickLinkDao.updateQuickLink(quickLink);

        // Assert
        verify(tenantTable).putItem(quickLink);
        assertEquals("QUICKLINK", result.getType());
        assertEquals("QUICKLINK#" + LINK_ID, result.getSk());
        assertNotNull(result);
    }

    @Test
    void testUpdateQuickLink_NullLinkId() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        quickLink.setLinkId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.updateQuickLink(quickLink));
        assertEquals("linkId must be provided for update", exception.getMessage());
    }

    @Test
    void testUpdateQuickLink_EmptyLinkId() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        quickLink.setLinkId("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.updateQuickLink(quickLink));
        assertEquals("linkId must be provided for update", exception.getMessage());
    }

    @Test
    void testUpdateQuickLink_DynamoDbException() {
        // Arrange
        Tenant quickLink = createTestQuickLink();
        quickLink.setLinkId(LINK_ID);
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        doThrow(DynamoDbException.builder().message("DynamoDB error").build())
                .when(tenantTable).putItem(any(Tenant.class));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.updateQuickLink(quickLink));
        assertTrue(exception.getMessage().contains("Error updating QuickLink in DynamoDB"));
    }

    @Test
    void testDeleteQuickLink_Success() {
        // Arrange
        String pk = TENANT_CODE;
        String sk = "QUICKLINK#" + LINK_ID;
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        // Act
        quickLinkDao.deleteQuickLink(pk, sk);

        // Assert
        verify(tenantTable).deleteItem(any(Consumer.class));
    }

    @Test
    void testDeleteQuickLink_NullPk() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.deleteQuickLink(null, "sk"));
        assertEquals("pk and sk must be provided for delete", exception.getMessage());
    }

    @Test
    void testDeleteQuickLink_EmptyPk() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.deleteQuickLink("", "sk"));
        assertEquals("pk and sk must be provided for delete", exception.getMessage());
    }

    @Test
    void testDeleteQuickLink_NullSk() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.deleteQuickLink("pk", null));
        assertEquals("pk and sk must be provided for delete", exception.getMessage());
    }

    @Test
    void testDeleteQuickLink_EmptySk() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> quickLinkDao.deleteQuickLink("pk", ""));
        assertEquals("pk and sk must be provided for delete", exception.getMessage());
    }

    @Test
    void testDeleteQuickLink_DynamoDbException() {
        // Arrange
        String pk = TENANT_CODE;
        String sk = "QUICKLINK#" + LINK_ID;
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        doThrow(DynamoDbException.builder().message("DynamoDB error").build())
                .when(tenantTable).deleteItem(any(Consumer.class));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.deleteQuickLink(pk, sk));
        assertTrue(exception.getMessage().contains("Error deleting QuickLink from DynamoDB"));
    }

    @Test
    void testGetQuickLinkByPk_Success() {
        // Arrange
        String pk = TENANT_CODE;
        String sk = "QUICKLINK#" + LINK_ID;
        Tenant expectedQuickLink = createTestQuickLink();
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.getItem(any(Consumer.class))).thenReturn(expectedQuickLink);

        // Act
        Tenant result = quickLinkDao.getQuickLinkByPk(pk, sk);

        // Assert
        assertNotNull(result);
        assertEquals(expectedQuickLink, result);
        verify(tenantTable).getItem(any(Consumer.class));
    }

    @Test
    void testGetQuickLinkByPk_NotFound() {
        // Arrange
        String pk = TENANT_CODE;
        String sk = "QUICKLINK#" + LINK_ID;
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.getItem(any(Consumer.class))).thenReturn(null);

        // Act
        Tenant result = quickLinkDao.getQuickLinkByPk(pk, sk);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetQuickLinkByPk_Exception() {
        // Arrange
        String pk = TENANT_CODE;
        String sk = "QUICKLINK#" + LINK_ID;
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.getItem(any(Consumer.class)))
                .thenThrow(new RuntimeException("Error"));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.getQuickLinkByPk(pk, sk));
        assertTrue(exception.getMessage().contains("Error fetching QuickLink by pk and sk"));
    }

    @Test
    void testUpdateQuickLinksTransactional_Success() {
        // Arrange
        List<Tenant> links = Arrays.asList(
                createTestQuickLinkWithId("link1", 1),
                createTestQuickLinkWithId("link2", 2)
        );

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        TableSchema<Tenant> mockSchema = mock(TableSchema.class);
        when(tenantTable.tableSchema()).thenReturn(mockSchema);
        when(mockSchema.itemToMap(any(Tenant.class), eq(true)))
                .thenReturn(new HashMap<>());

        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenReturn(TransactWriteItemsResponse.builder().build());

        // Act
        quickLinkDao.updateQuickLinksTransactional(links);

        // Assert
        ArgumentCaptor<TransactWriteItemsRequest> captor =
                ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(dynamoDbClient).transactWriteItems(captor.capture());
        assertEquals(2, captor.getValue().transactItems().size());

        // Verify that type and sk were set
        for (Tenant link : links) {
            assertEquals("QUICKLINK", link.getType());
            assertTrue(link.getSk().startsWith("QUICKLINK#"));
        }
    }

    @Test
    void testUpdateQuickLinksTransactional_DynamoDbException() {
        // Arrange
        List<Tenant> links = Arrays.asList(createTestQuickLinkWithId("link1", 1));

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        TableSchema<Tenant> mockSchema = mock(TableSchema.class);
        when(tenantTable.tableSchema()).thenReturn(mockSchema);
        when(mockSchema.itemToMap(any(Tenant.class), eq(true)))
                .thenReturn(new HashMap<>());

        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Transaction failed").build());

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.updateQuickLinksTransactional(links));
        assertTrue(exception.getMessage().contains("Error in transactional update"));
    }

    @Test
    void testUpdateQuickLinksTransactional_UnexpectedException() {
        // Arrange
        List<Tenant> links = Arrays.asList(createTestQuickLinkWithId("link1", 1));

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);

        TableSchema<Tenant> mockSchema = mock(TableSchema.class);
        when(tenantTable.tableSchema()).thenReturn(mockSchema);
        when(mockSchema.itemToMap(any(Tenant.class), eq(true)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.updateQuickLinksTransactional(links));
        assertTrue(exception.getMessage().contains("Unexpected error in transactional update"));
    }

    @Test
    void testGetQuickLinksByTenant_Success() {
        // Arrange
        Tenant link1 = createTestQuickLinkWithId("link1", 1);
        link1.setPk(TENANT_CODE);
        link1.setSk("QUICKLINK#link1");

        Tenant link2 = createTestQuickLinkWithId("link2", 2);
        link2.setPk(TENANT_CODE);
        link2.setSk("QUICKLINK#link2");

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(Arrays.asList(link1, link2));

        Stream<Page<Tenant>> pageStream = Stream.of(page);
        when(gsiTypeIndex.query(any(QueryConditional.class))).thenReturn(() -> pageStream.iterator());

        // Act
        List<Tenant> results = quickLinkDao.getQuickLinksByTenant(TENANT_CODE);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getIndex());
        assertEquals(2, results.get(1).getIndex());
    }

    @Test
    void testGetQuickLinksByTenant_WithSorting() {
        // Arrange
        Tenant link1 = createTestQuickLinkWithId("link1", 3);
        link1.setPk(TENANT_CODE);
        link1.setSk("QUICKLINK#link1");

        Tenant link2 = createTestQuickLinkWithId("link2", 1);
        link2.setPk(TENANT_CODE);
        link2.setSk("QUICKLINK#link2");

        Tenant link3 = createTestQuickLinkWithId("link3", 2);
        link3.setPk(TENANT_CODE);
        link3.setSk("QUICKLINK#link3");

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(Arrays.asList(link1, link2, link3));

        Stream<Page<Tenant>> pageStream = Stream.of(page);
        when(gsiTypeIndex.query(any(QueryConditional.class))).thenReturn(() -> pageStream.iterator());

        // Act
        List<Tenant> results = quickLinkDao.getQuickLinksByTenant(TENANT_CODE);

        // Assert
        assertEquals(3, results.size());
        assertEquals(1, results.get(0).getIndex()); // link2
        assertEquals(2, results.get(1).getIndex()); // link3
        assertEquals(3, results.get(2).getIndex()); // link1
    }

    @Test
    void testGetQuickLinksByTenant_FiltersByTenantCode() {
        // Arrange
        Tenant link1 = createTestQuickLinkWithId("link1", 1);
        link1.setPk(TENANT_CODE);
        link1.setSk("QUICKLINK#link1");

        Tenant link2 = createTestQuickLinkWithId("link2", 2);
        link2.setPk("OTHER_TENANT");
        link2.setSk("QUICKLINK#link2");

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(Arrays.asList(link1, link2));

        Stream<Page<Tenant>> pageStream = Stream.of(page);
        when(gsiTypeIndex.query(any(QueryConditional.class))).thenReturn(() -> pageStream.iterator());

        // Act
        List<Tenant> results = quickLinkDao.getQuickLinksByTenant(TENANT_CODE);

        // Assert
        assertEquals(1, results.size());
        assertEquals(TENANT_CODE, results.get(0).getPk());
    }

    @Test
    void testGetQuickLinksByTenant_FiltersNonQuickLinks() {
        // Arrange
        Tenant link1 = createTestQuickLinkWithId("link1", 1);
        link1.setPk(TENANT_CODE);
        link1.setSk("QUICKLINK#link1");

        Tenant otherType = createTestQuickLinkWithId("other", 2);
        otherType.setPk(TENANT_CODE);
        otherType.setSk("OTHER#type");

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(Arrays.asList(link1, otherType));

        Stream<Page<Tenant>> pageStream = Stream.of(page);
        when(gsiTypeIndex.query(any(QueryConditional.class))).thenReturn(() -> pageStream.iterator());

        // Act
        List<Tenant> results = quickLinkDao.getQuickLinksByTenant(TENANT_CODE);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getSk().startsWith("QUICKLINK#"));
    }

    @Test
    void testGetQuickLinksByTenant_HandlesNullIndex() {
        // Arrange
        Tenant link1 = createTestQuickLinkWithId("link1", null);
        link1.setPk(TENANT_CODE);
        link1.setSk("QUICKLINK#link1");

        Tenant link2 = createTestQuickLinkWithId("link2", 1);
        link2.setPk(TENANT_CODE);
        link2.setSk("QUICKLINK#link2");

        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(Arrays.asList(link1, link2));

        Stream<Page<Tenant>> pageStream = Stream.of(page);
        when(gsiTypeIndex.query(any(QueryConditional.class))).thenReturn(() -> pageStream.iterator());

        // Act
        List<Tenant> results = quickLinkDao.getQuickLinksByTenant(TENANT_CODE);

        // Assert
        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getIndex()); // link2 first
        assertNull(results.get(1).getIndex()); // link1 last
    }

    @Test
    void testGetQuickLinksByTenant_DynamoDbException() {
        // Arrange
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);
        when(gsiTypeIndex.query(any(QueryConditional.class)))
                .thenThrow(DynamoDbException.builder().message("Query failed").build());

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.getQuickLinksByTenant(TENANT_CODE));
        assertTrue(exception.getMessage().contains("Error fetching QuickLinks by tenant from DynamoDB"));
    }

    @Test
    void testGetQuickLinksByTenant_UnexpectedException() {
        // Arrange
        when(dynamoDbEnhancedClient.table(eq(TENANT_TABLE_NAME), any(TableSchema.class)))
                .thenReturn(tenantTable);
        when(tenantTable.index("gsi_type")).thenReturn(gsiTypeIndex);
        when(gsiTypeIndex.query(any(QueryConditional.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        DataBaseException exception = assertThrows(DataBaseException.class,
                () -> quickLinkDao.getQuickLinksByTenant(TENANT_CODE));
        assertTrue(exception.getMessage().contains("Unexpected error fetching QuickLinks by tenant"));
    }

    // Helper methods
    private Tenant createTestQuickLink() {
        Tenant tenant = new Tenant();
        tenant.setPk(TENANT_CODE);
        tenant.setLinkId(LINK_ID);
        tenant.setSk("QUICKLINK#" + LINK_ID);
        return tenant;
    }

    private Tenant createTestQuickLinkWithId(String linkId, Integer index) {
        Tenant tenant = new Tenant();
        tenant.setPk(TENANT_CODE);
        tenant.setLinkId(linkId);
        tenant.setSk("QUICKLINK#" + linkId);
        tenant.setIndex(index);
        return tenant;
    }
}