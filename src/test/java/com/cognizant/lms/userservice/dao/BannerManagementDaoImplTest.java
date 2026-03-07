package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.exception.DataBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BannerManagementDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbTable<Tenant> mockTable;

    private BannerManagementDaoImpl bannerManagementDao;

    private Tenant testTenant;
    private final String tableName = "test-tenant-table";

    @BeforeEach
    void setUp() {
        // Set up test data with all required fields for GSI structure
        // Note: type field is NOT set by DAO to prevent dual GSI storage
        testTenant = new Tenant();
        testTenant.setPk("t-123");
        testTenant.setSk("BANNER#test-banner-id");
        testTenant.setBannerId("BANNER#test-banner-id");
        testTenant.setBannerTitle("Test Banner");
        testTenant.setBannerDescription("Test Description");
        testTenant.setBannerStatus("ACTIVE");
        testTenant.setStartDate("2024-01-01"); // Required for gsi_banner_type (pk=pk, sk=startDate)
        testTenant.setEndDate("2024-12-31");
        testTenant.setBannerHeading("Test Heading");
        testTenant.setBannerSubHeading("Test Sub Heading");
        testTenant.setBannerRedirectionUrl("https://example.com");
        testTenant.setBannerImageKey("test-image-key");
        testTenant.setCreatedBy("testUser");
        testTenant.setCreatedOn("2024-01-01T00:00:00Z");

        // Mock the enhanced client and table
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(dynamoDbEnhancedClient);
        when(dynamoDbEnhancedClient.table(eq(tableName), any(TableSchema.class))).thenReturn(mockTable);

        // Create DAO instance manually with mocks
        bannerManagementDao = new BannerManagementDaoImpl(dynamoDBConfig, tableName, dynamoDbClient);
    }

    @Test
    void testSaveBannerManagementIcon_Success() {
        // Given
        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(testTenant);

        // Then
        verify(mockTable, times(1)).putItem(argThat((Tenant tenant) -> {
            assertEquals(testTenant.getPk(), tenant.getPk());
            assertEquals(testTenant.getSk(), tenant.getSk());
            assertEquals(testTenant.getBannerId(), tenant.getBannerId());
            assertEquals(testTenant.getBannerTitle(), tenant.getBannerTitle());
            assertEquals(testTenant.getBannerDescription(), tenant.getBannerDescription());
            assertEquals(testTenant.getBannerStatus(), tenant.getBannerStatus());
            // Verify that type is not set by the DAO
            assertNull(tenant.getType());
            return true;
        }));

        // Verify that the type is NOT set (DAO no longer sets type field to prevent dual GSI storage)
        assertNull(testTenant.getType());
    }

    @Test
    void testSaveBannerManagementIcon_DynamoDbException() {
        // Given

        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
            .message("DynamoDB operation failed")
            .build();
        doThrow(dynamoException).when(mockTable).putItem(any(Tenant.class));

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.saveBannerManagementIcon(testTenant));

        assertTrue(exception.getMessage().contains("Error saving BannerManagement to DynamoDB"));
        assertTrue(exception.getMessage().contains("DynamoDB operation failed"));

        verify(mockTable, times(1)).putItem(any(Tenant.class));
    }

    @Test
    void testSaveBannerManagementIcon_GenericException() {
        // Given

        RuntimeException runtimeException = new RuntimeException("Unexpected error occurred");
        doThrow(runtimeException).when(mockTable).putItem(any(Tenant.class));

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.saveBannerManagementIcon(testTenant));

        assertTrue(exception.getMessage().contains("Unexpected error saving BannerManagement"));
        assertTrue(exception.getMessage().contains("Unexpected error occurred"));

        verify(mockTable, times(1)).putItem(any(Tenant.class));
    }

    @Test
    void testSaveBannerManagementIcon_NullPointerException() {
        // Given

        NullPointerException npe = new NullPointerException("Null reference");
        doThrow(npe).when(mockTable).putItem(any(Tenant.class));

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.saveBannerManagementIcon(testTenant));

        assertTrue(exception.getMessage().contains("Unexpected error saving BannerManagement"));
        assertTrue(exception.getMessage().contains("Null reference"));

        verify(mockTable, times(1)).putItem(any(Tenant.class));
    }

    @Test
    void testSaveBannerManagementIcon_TypeNotSetForBanners() {
        // Given
        Tenant tenantWithoutType = new Tenant();
        tenantWithoutType.setPk("t-456");
        tenantWithoutType.setSk("BANNER#another-banner");
        tenantWithoutType.setBannerId("BANNER#another-banner");
        tenantWithoutType.setBannerTitle("Another Banner");
        // Type is not set initially
        assertNull(tenantWithoutType.getType());

        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(tenantWithoutType);

        // Then - Type should remain null (DAO no longer sets it to prevent dual GSI storage)
        assertNull(tenantWithoutType.getType());
        verify(mockTable, times(1)).putItem(tenantWithoutType);
    }

    @Test
    void testSaveBannerManagementIcon_AllFieldsPreserved() {
        // Given
        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(testTenant);

        // Then
        verify(mockTable, times(1)).putItem(argThat((Tenant tenant) -> {
            // Verify all important fields are preserved
            assertEquals(testTenant.getPk(), tenant.getPk());
            assertEquals(testTenant.getSk(), tenant.getSk());
            assertEquals(testTenant.getBannerId(), tenant.getBannerId());
            assertEquals(testTenant.getBannerTitle(), tenant.getBannerTitle());
            assertEquals(testTenant.getBannerDescription(), tenant.getBannerDescription());
            assertEquals(testTenant.getBannerStatus(), tenant.getBannerStatus());
            assertEquals(testTenant.getStartDate(), tenant.getStartDate());
            assertEquals(testTenant.getEndDate(), tenant.getEndDate());
            assertEquals(testTenant.getBannerHeading(), tenant.getBannerHeading());
            assertEquals(testTenant.getBannerSubHeading(), tenant.getBannerSubHeading());
            assertEquals(testTenant.getBannerRedirectionUrl(), tenant.getBannerRedirectionUrl());
            assertEquals(testTenant.getBannerImageKey(), tenant.getBannerImageKey());
            assertEquals(testTenant.getCreatedBy(), tenant.getCreatedBy());
            assertEquals(testTenant.getCreatedOn(), tenant.getCreatedOn());
            // Type should NOT be set for banner records (DAO no longer sets it)
            assertNull(tenant.getType());
            return true;
        }));
    }

    @Test
    void testSaveBannerManagementIcon_EmptyTenant() {
        // Given
        Tenant emptyTenant = new Tenant();
        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(emptyTenant);

        // Then - Type should remain null (DAO no longer sets it)
        assertNull(emptyTenant.getType());
        verify(mockTable, times(1)).putItem(emptyTenant);
    }

    @Test
    void testSaveBannerManagementIcon_DynamoDbExceptionWithNullMessage() {
        // Given

        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
            .message(null)
            .build();
        doThrow(dynamoException).when(mockTable).putItem(any(Tenant.class));

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.saveBannerManagementIcon(testTenant));

        assertTrue(exception.getMessage().contains("Error saving BannerManagement to DynamoDB"));
        verify(mockTable, times(1)).putItem(any(Tenant.class));
    }

    @Test
    void testSaveBannerManagementIcon_RuntimeExceptionWithNullMessage() {
        // Given

        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(mockTable).putItem(any(Tenant.class));

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.saveBannerManagementIcon(testTenant));

        assertTrue(exception.getMessage().contains("Unexpected error saving BannerManagement"));
        verify(mockTable, times(1)).putItem(any(Tenant.class));
    }

    @Test
    void testSaveBannerManagementIcon_VerifyTableSchemaUsage() {
        // Given
        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(testTenant);

        // Then
        verify(mockTable, times(1)).putItem(testTenant);
        verify(dynamoDbEnhancedClient, times(1)).table(eq(tableName), any(TableSchema.class));
    }

    @Test
    void testSaveBannerManagementIcon_MultipleCallsIndependent() {
        // Given - Multiple tenants with required fields for GSI
        Tenant tenant1 = new Tenant();
        tenant1.setPk("t-001");
        tenant1.setSk("BANNER#001");
        tenant1.setBannerId("BANNER#001");
        tenant1.setBannerTitle("Banner 1");
        tenant1.setBannerStatus("ACTIVE");
        tenant1.setStartDate("2024-01-01"); // Required for gsi_banner_type

        Tenant tenant2 = new Tenant();
        tenant2.setPk("t-002");
        tenant2.setSk("BANNER#002");
        tenant2.setBannerId("BANNER#002");
        tenant2.setBannerTitle("Banner 2");
        tenant2.setBannerStatus("INACTIVE");
        tenant2.setStartDate("2024-02-01"); // Required for gsi_banner_type

        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(tenant1);
        bannerManagementDao.saveBannerManagementIcon(tenant2);

        // Then - Type should remain null for both tenants (DAO no longer sets it)
        assertNull(tenant1.getType());
        assertNull(tenant2.getType());

        verify(mockTable, times(2)).putItem(any(Tenant.class));
        verify(dynamoDbEnhancedClient, times(2)).table(eq(tableName), any(TableSchema.class));
    }

    @Test
    void testCountActiveBanners_Success() {
        // Given
        String tenantCode = "t-123";

        // Create mock scan results - banners are identified by pk/sk structure
        Tenant activeBanner1 = new Tenant();
        activeBanner1.setPk(tenantCode);
        activeBanner1.setSk("BANNER#banner-1");
        activeBanner1.setBannerId("BANNER#banner-1");
        activeBanner1.setBannerStatus("ACTIVE");
        activeBanner1.setStartDate("2024-01-01");

        Tenant activeBanner2 = new Tenant();
        activeBanner2.setPk(tenantCode);
        activeBanner2.setSk("BANNER#banner-2");
        activeBanner2.setBannerId("BANNER#banner-2");
        activeBanner2.setBannerStatus("ACTIVE");
        activeBanner2.setStartDate("2024-02-01");

        Tenant inactiveBanner = new Tenant();
        inactiveBanner.setPk(tenantCode);
        inactiveBanner.setSk("BANNER#banner-3");
        inactiveBanner.setBannerId("BANNER#banner-3");
        inactiveBanner.setBannerStatus("INACTIVE");
        inactiveBanner.setStartDate("2024-03-01");

        Tenant otherTenantBanner = new Tenant();
        otherTenantBanner.setPk("t-456");
        otherTenantBanner.setSk("BANNER#banner-4");
        otherTenantBanner.setBannerId("BANNER#banner-4");
        otherTenantBanner.setBannerStatus("ACTIVE");
        otherTenantBanner.setStartDate("2024-04-01");

        // Mock scan operation - uses main table scan, not GSI
        @SuppressWarnings("unchecked")
        software.amazon.awssdk.enhanced.dynamodb.model.PageIterable<Tenant> pageIterable =
            mock(software.amazon.awssdk.enhanced.dynamodb.model.PageIterable.class);

        when(mockTable.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() ->
            java.util.Arrays.asList(activeBanner1, activeBanner2, inactiveBanner, otherTenantBanner).iterator());

        // When
        int result = bannerManagementDao.countActiveBanners(tenantCode);

        // Then
        assertEquals(2, result); // Only 2 active banners for the specific tenant
        verify(mockTable, times(1)).scan();
    }

    @Test
    void testCountActiveBanners_NoActiveBanners() {
        // Given
        String tenantCode = "t-123";

        // Mock scan operation returning empty results
        @SuppressWarnings("unchecked")
        software.amazon.awssdk.enhanced.dynamodb.model.PageIterable<Tenant> pageIterable =
            mock(software.amazon.awssdk.enhanced.dynamodb.model.PageIterable.class);

        when(mockTable.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(java.util.Collections::emptyIterator);

        // When
        int result = bannerManagementDao.countActiveBanners(tenantCode);

        // Then
        assertEquals(0, result);
        verify(mockTable, times(1)).scan();
    }

    @Test
    void testCountActiveBanners_DynamoDbException() {
        // Given
        String tenantCode = "t-123";
        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
            .message("Scan operation failed")
            .build();
        when(mockTable.scan()).thenThrow(dynamoException);

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.countActiveBanners(tenantCode));

        assertTrue(exception.getMessage().contains("Error counting active banners in DynamoDB"));
        assertTrue(exception.getMessage().contains("Scan operation failed"));
        verify(mockTable, times(1)).scan();
    }

    @Test
    void testCountActiveBanners_GenericException() {
        // Given
        String tenantCode = "t-123";
        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        when(mockTable.scan()).thenThrow(runtimeException);

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.countActiveBanners(tenantCode));

        assertTrue(exception.getMessage().contains("Unexpected error counting active banners"));
        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(mockTable, times(1)).scan();
    }

    @Test
    void testSaveBannerManagementIcon_ComplexBannerData() {
        // Given - Complex banner with all fields required for new GSI structure
        Tenant complexTenant = new Tenant();
        complexTenant.setPk("tenant-complex");
        complexTenant.setSk("BANNER#complex-banner-123");
        complexTenant.setBannerId("BANNER#complex-banner-123");
        complexTenant.setBannerTitle("Complex Marketing Banner");
        complexTenant.setBannerDescription("This is a comprehensive banner with detailed marketing information");
        complexTenant.setBannerStatus("ACTIVE");
        complexTenant.setStartDate("2024-11-04"); // Required for gsi_banner_type (pk=pk, sk=startDate)
        complexTenant.setEndDate("2025-01-31");
        complexTenant.setBannerHeading("Holiday Special Offers");
        complexTenant.setBannerSubHeading("Limited Time Only - Save Up to 70%");
        complexTenant.setBannerRedirectionUrl("https://example.com/holiday-offers?utm_source=banner&utm_campaign=holiday2024");
        complexTenant.setBannerImageKey("s3://banner-images/holiday-special-2024.jpg");
        complexTenant.setCreatedBy("marketing-admin");
        complexTenant.setCreatedOn("2024-11-04T10:30:00Z");

        doNothing().when(mockTable).putItem(any(Tenant.class));

        // When
        bannerManagementDao.saveBannerManagementIcon(complexTenant);

        // Then - Type should remain null (DAO no longer sets it)
        assertNull(complexTenant.getType());

        verify(mockTable, times(1)).putItem(argThat((Tenant tenant) -> {
            assertEquals("tenant-complex", tenant.getPk());
            assertEquals("BANNER#complex-banner-123", tenant.getSk());
            assertEquals("Complex Marketing Banner", tenant.getBannerTitle());
            assertEquals("This is a comprehensive banner with detailed marketing information", tenant.getBannerDescription());
            assertEquals("ACTIVE", tenant.getBannerStatus());
            assertEquals("2024-11-04", tenant.getStartDate());
            assertEquals("2025-01-31", tenant.getEndDate());
            assertEquals("Holiday Special Offers", tenant.getBannerHeading());
            assertEquals("Limited Time Only - Save Up to 70%", tenant.getBannerSubHeading());
            assertEquals("https://example.com/holiday-offers?utm_source=banner&utm_campaign=holiday2024",
                        tenant.getBannerRedirectionUrl());
            assertEquals("s3://banner-images/holiday-special-2024.jpg", tenant.getBannerImageKey());
            assertEquals("marketing-admin", tenant.getCreatedBy());
            assertEquals("2024-11-04T10:30:00Z", tenant.getCreatedOn());
            // Type should NOT be set for banner records (DAO no longer sets it)
            assertNull(tenant.getType());
            return true;
        }));
    }

    @Test
    void testGetBannerById_Success() {
        // Given
        String tenantCode = "t-123";
        String bannerId = "BANNER#test-banner";

        Tenant expectedBanner = new Tenant();
        expectedBanner.setPk(tenantCode);
        expectedBanner.setSk("BANNER#test-banner");
        expectedBanner.setBannerId(bannerId);
        expectedBanner.setBannerTitle("Test Banner");
        expectedBanner.setBannerStatus("ACTIVE");
        expectedBanner.setStartDate("2024-01-01"); // Required for gsi_banner_type
        expectedBanner.setEndDate("2024-12-31");
        expectedBanner.setBannerHeading("Test Heading");
        expectedBanner.setBannerSubHeading("Test Sub Heading");
        expectedBanner.setBannerRedirectionUrl("https://example.com");

        software.amazon.awssdk.enhanced.dynamodb.Key expectedKey = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(tenantCode)
            .sortValue("BANNER#test-banner")
            .build();

        when(mockTable.getItem(expectedKey)).thenReturn(expectedBanner);

        // When
        Tenant result = bannerManagementDao.getBannerById(tenantCode, bannerId);

        // Then
        assertNotNull(result);
        assertEquals(tenantCode, result.getPk());
        assertEquals("BANNER#test-banner", result.getSk());
        assertEquals(bannerId, result.getBannerId());
        assertEquals("Test Banner", result.getBannerTitle());
        assertEquals("ACTIVE", result.getBannerStatus());
        assertEquals("2024-01-01", result.getStartDate());

        verify(mockTable, times(1)).getItem(expectedKey);
    }

    @Test
    void testGetBannerById_BannerNotFound() {
        // Given
        String tenantCode = "t-123";
        String bannerId = "BANNER#non-existent";

        software.amazon.awssdk.enhanced.dynamodb.Key expectedKey = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(tenantCode)
            .sortValue("BANNER#non-existent")
            .build();

        when(mockTable.getItem(expectedKey)).thenReturn(null);

        // When
        Tenant result = bannerManagementDao.getBannerById(tenantCode, bannerId);

        // Then
        assertNull(result);
        verify(mockTable, times(1)).getItem(expectedKey);
    }

    @Test
    void testGetBannerById_WithBannerIdWithoutPrefix() {
        // Given
        String tenantCode = "t-123";
        String bannerId = "test-banner"; // Without BANNER# prefix

        Tenant expectedBanner = new Tenant();
        expectedBanner.setPk(tenantCode);
        expectedBanner.setSk("BANNER#test-banner");
        expectedBanner.setBannerId("BANNER#test-banner");
        expectedBanner.setBannerStatus("ACTIVE");
        expectedBanner.setStartDate("2024-01-01"); // Required for gsi_banner_type

        software.amazon.awssdk.enhanced.dynamodb.Key expectedKey = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(tenantCode)
            .sortValue("BANNER#test-banner")
            .build();

        when(mockTable.getItem(expectedKey)).thenReturn(expectedBanner);

        // When
        Tenant result = bannerManagementDao.getBannerById(tenantCode, bannerId);

        // Then
        assertNotNull(result);
        assertEquals("BANNER#test-banner", result.getSk());
        assertEquals("2024-01-01", result.getStartDate());
        verify(mockTable, times(1)).getItem(expectedKey);
    }

    @Test
    void testGetBannerById_DynamoDbException() {
        // Given
        String tenantCode = "t-123";
        String bannerId = "BANNER#test-banner";

        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
            .message("Get operation failed")
            .build();

        software.amazon.awssdk.enhanced.dynamodb.Key expectedKey = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(tenantCode)
            .sortValue("BANNER#test-banner")
            .build();

        when(mockTable.getItem(expectedKey)).thenThrow(dynamoException);

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.getBannerById(tenantCode, bannerId));

        assertTrue(exception.getMessage().contains("Error getting banner from DynamoDB"));
        assertTrue(exception.getMessage().contains("Get operation failed"));
        verify(mockTable, times(1)).getItem(expectedKey);
    }

    @Test
    void testGetBannerById_GenericException() {
        // Given
        String tenantCode = "t-123";
        String bannerId = "BANNER#test-banner";

        RuntimeException runtimeException = new RuntimeException("Unexpected error");

        software.amazon.awssdk.enhanced.dynamodb.Key expectedKey = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(tenantCode)
            .sortValue("BANNER#test-banner")
            .build();

        when(mockTable.getItem(expectedKey)).thenThrow(runtimeException);

        // When & Then
        DataBaseException exception = assertThrows(DataBaseException.class,
            () -> bannerManagementDao.getBannerById(tenantCode, bannerId));

        assertTrue(exception.getMessage().contains("Unexpected error getting banner"));
        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(mockTable, times(1)).getItem(expectedKey);
    }

    @Test
    void testGetBannersByTenant_SuccessFiltersAndSorts() {
        String tenantCode = "t-123";
        Tenant bannerInvalidDate = new Tenant();
        bannerInvalidDate.setPk(tenantCode);
        bannerInvalidDate.setSk("BANNER#1");
        bannerInvalidDate.setCreatedOn("not-an-instant");

        Tenant bannerOld = new Tenant();
        bannerOld.setPk(tenantCode);
        bannerOld.setSk("BANNER#2");
        bannerOld.setCreatedOn("2024-01-01T00:00:00Z");

        Tenant bannerNew = new Tenant();
        bannerNew.setPk(tenantCode);
        bannerNew.setSk("BANNER#3");
        bannerNew.setCreatedOn("2024-02-01T00:00:00Z");

        Tenant otherSk = new Tenant();
        otherSk.setPk(tenantCode);
        otherSk.setSk("NOTBANNER#x");
        otherSk.setCreatedOn("2024-03-01T00:00:00Z");

        Page<Tenant> page1 = mock(Page.class);
        when(page1.items()).thenReturn(java.util.List.of(bannerNew, otherSk));
        Page<Tenant> page2 = mock(Page.class);
        when(page2.items()).thenReturn(java.util.List.of(bannerOld, bannerInvalidDate));

        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenReturn(pageIterable);
        when(pageIterable.stream()).thenReturn(Stream.of(page1, page2));

        java.util.List<Tenant> result = bannerManagementDao.getBannersByTenant(tenantCode);
        assertEquals(3, result.size());
        assertEquals("BANNER#1", result.get(0).getSk());
        assertEquals("BANNER#2", result.get(1).getSk());
        assertEquals("BANNER#3", result.get(2).getSk());
        assertTrue(result.stream().noneMatch(t -> "NOTBANNER#x".equals(t.getSk())));
        verify(mockTable, times(1)).query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any());
    }

    @Test
    void testGetBannersByTenant_EmptyResult() {
        String tenantCode = "t-empty";
        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenReturn(pageIterable);
        when(pageIterable.stream()).thenReturn(Stream.empty());
        java.util.List<Tenant> result = bannerManagementDao.getBannersByTenant(tenantCode);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockTable, times(1)).query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any());
    }

    @Test
    void testGetBannersByTenant_IgnoresNullAndNonBannerSk() {
        String tenantCode = "t-456";
        Tenant bannerOk = new Tenant();
        bannerOk.setPk(tenantCode);
        bannerOk.setSk("BANNER#good");
        bannerOk.setCreatedOn("2024-03-10T00:00:00Z");

        Tenant nullSk = new Tenant();
        nullSk.setPk(tenantCode);

        Tenant nonBanner = new Tenant();
        nonBanner.setPk(tenantCode);
        nonBanner.setSk("FILE#abc");

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(java.util.List.of(bannerOk, nullSk, nonBanner));

        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenReturn(pageIterable);
        when(pageIterable.stream()).thenReturn(Stream.of(page));

        java.util.List<Tenant> result = bannerManagementDao.getBannersByTenant(tenantCode);
        assertEquals(1, result.size());
        assertEquals("BANNER#good", result.get(0).getSk());
    }

    @Test
    void testGetBannersByTenant_DynamoDbException() {
        String tenantCode = "t-err";
        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
                .message("Query failed").build();
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenThrow(dynamoException);
        DataBaseException ex = assertThrows(DataBaseException.class,
                () -> bannerManagementDao.getBannersByTenant(tenantCode));
        assertTrue(ex.getMessage().contains("Error fetching Banners by tenant"));
        assertTrue(ex.getMessage().contains("Query failed"));
        verify(mockTable, times(1)).query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any());
    }

    @Test
    void testGetBannersByTenant_GenericException() {
        String tenantCode = "t-gen";
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenThrow(new RuntimeException("Boom"));
        DataBaseException ex = assertThrows(DataBaseException.class,
                () -> bannerManagementDao.getBannersByTenant(tenantCode));
        assertTrue(ex.getMessage().contains("Unexpected error fetching Banners by tenant"));
        assertTrue(ex.getMessage().contains("Boom"));
        verify(mockTable, times(1)).query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any());
    }

    @Test
    void testGetBannersByTenant_SortingStableWhenSameCreatedOn() {
        String tenantCode = "t-stable";
        Tenant b1 = new Tenant();
        b1.setPk(tenantCode);
        b1.setSk("BANNER#a");
        b1.setCreatedOn("2024-01-01T00:00:00Z");

        Tenant b2 = new Tenant();
        b2.setPk(tenantCode);
        b2.setSk("BANNER#b");
        b2.setCreatedOn("2024-01-01T00:00:00Z");

        Page<Tenant> page = mock(Page.class);
        when(page.items()).thenReturn(java.util.List.of(b2, b1));
        PageIterable<Tenant> pageIterable = mock(PageIterable.class);
        when(mockTable.query(Mockito.<Consumer<QueryEnhancedRequest.Builder>>any())).thenReturn(pageIterable);
        when(pageIterable.stream()).thenReturn(Stream.of(page));

        java.util.List<Tenant> result = bannerManagementDao.getBannersByTenant(tenantCode);
        assertEquals(2, result.size());
        assertEquals("BANNER#b", result.get(0).getSk());
        assertEquals("BANNER#a", result.get(1).getSk());
    }

}
