package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MessageDaoImplTest {

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbTable<Tenant> tenantTable;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @InjectMocks
    private MessageDaoImpl messageDao;

    private final String tenantCode = "t-2";
    private final String category = "Alert";
    private final String sk = tenantCode + "#" + category;
    private final String tableName = "Tenants";

    @BeforeEach
    void setUp() {
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        when(dynamoDbEnhancedClient.table(eq(tableName), any(TableSchema.class)))
                .thenReturn(tenantTable);
        messageDao = new MessageDaoImpl(dynamoDbEnhancedClient, dynamoDBConfig, tableName);
    }

    @Test
    void testSaveTenant() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setSk(sk);
        tenant.setCategory(category);
        tenant.setMessage("Maintenance");

        messageDao.saveTenant(tenant);

        verify(tenantTable, times(1)).putItem(tenant);
    }

    @Test
    void testFindTenantByKey_TenantExists() {
        when(tenantTable.getItem(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<GetItemEnhancedRequest.Builder> consumer = invocation.getArgument(0);
            GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
            consumer.accept(builder);

            Key key = builder.build().key();
            assertEquals(tenantCode, key.partitionKeyValue().s());

            Tenant tenant = new Tenant();
            tenant.setPk(tenantCode);
            tenant.setSk(sk);
            tenant.setCategory(category);
            return tenant;
        });

        Tenant result = messageDao.findTenantByKey(tenantCode, category);

        assertNotNull(result);
        assertEquals(tenantCode, result.getPk());
        assertEquals(sk, result.getSk());
        assertEquals(category, result.getCategory());
    }

    @Test
    void testFindTenantByKey_TenantDoesNotExist() {
        when(tenantTable.getItem(any(Consumer.class))).thenReturn(null);
        Tenant result = messageDao.findTenantByKey(tenantCode, category);
        assertNull(result);
    }

    @Test
    void testFindTenantByCategoryAndStatus_CategoryAndStatusMatch() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setSk(sk);
        tenant.setCategory(category);
        tenant.setMessageStatus(true);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, category, true);
        assertEquals(1, result.size());
        assertEquals(tenantCode, result.get(0).getPk());
        assertEquals(true, result.get(0).getMessageStatus());
    }

    @Test
    void testFindTenantByCategoryAndStatus_CategoryMatchStatusMismatch() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setSk(sk);
        tenant.setCategory(category);
        tenant.setMessageStatus(false);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, category, true);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindTenantByCategoryAndStatus_OnlyCategoryProvided() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setSk(sk);
        tenant.setCategory(category);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, category, null);
        assertEquals(1, result.size());
        assertEquals(category, result.get(0).getCategory());
    }

    @Test
    void testFindTenantByCategoryAndStatus_OnlyStatusProvided() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setMessageStatus(true);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, null, true);
        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getMessageStatus());
    }

    @Test
    void testFindTenantByCategoryAndStatus_NeitherCategoryNorStatusProvided() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setCategory("Alert");
        tenant.setMessage("Maintenance");
        tenant.setMessageStatus(true);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, null, null);
        assertEquals(1, result.size());
        assertEquals("Alert", result.get(0).getCategory());
        assertEquals("Maintenance", result.get(0).getMessage());
        assertEquals(true, result.get(0).getMessageStatus());
    }

    @Test
    void testFindTenantByCategoryAndStatus_NeitherCategoryNorStatusProvided_MessageMissing() {
        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setCategory("Alert");
        tenant.setMessage(null);
        tenant.setMessageStatus(true);

        Page<Tenant> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(tenant));

        SdkIterable<Page<Tenant>> sdkIterable = () -> List.of(mockPage).iterator();
        PageIterable<Tenant> pageIterable = PageIterable.create(sdkIterable);

        when(tenantTable.query(any(Consumer.class))).thenReturn(pageIterable);

        List<Tenant> result = messageDao.findTenantByCategoryAndStatus(tenantCode, null, null);
        assertTrue(result.isEmpty());
    }
}






