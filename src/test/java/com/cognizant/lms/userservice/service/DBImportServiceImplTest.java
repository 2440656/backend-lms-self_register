package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.TableDisplayNameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DBImportServiceImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @InjectMocks
    private DBImportServiceImpl dbImportServiceImpl;

    private final String rolesTable = "rolesTable";
    private final String lookupTable = "lookupTable";
    private final String skillLookupsTable = "skillLookupsTable";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        dbImportServiceImpl = new DBImportServiceImpl(
            dynamoDBConfig,
            rolesTable,
            lookupTable,
            skillLookupsTable
        );
    }

    @Test
    void testDbImportdata() throws Exception {
        String csvContent = "pk,sk\n1,1\n2,2";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", inputStream);

        BatchWriteItemResponse mockResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(Collections.emptyMap())
                .build();
        when(dynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class))).thenReturn(mockResponse);

        int importedCount = dbImportServiceImpl.dbImportdata(file, "Roles Master Table");
        assertEquals(2, importedCount);

        ArgumentCaptor<BatchWriteItemRequest> captor = ArgumentCaptor.forClass(BatchWriteItemRequest.class);
        verify(dynamoDbClient, atLeastOnce()).batchWriteItem(captor.capture());

        BatchWriteItemRequest capturedRequest = captor.getValue();
        System.out.println("Captured Request: " + capturedRequest);
    }
    @Test
    void testGetTableDataAsCSV() throws IOException {
        String tableName = "Skill Lookups Master Table";
        List<Map<String, AttributeValue>> items = new ArrayList<>();
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("pk", AttributeValue.builder().s("1").build());
        item1.put("sk", AttributeValue.builder().s("1").build());
        items.add(item1);

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("pk", AttributeValue.builder().s("2").build());
        item2.put("sk", AttributeValue.builder().s("2").build());
        items.add(item2);

        ScanResponse mockResponse = ScanResponse.builder()
            .items(items)
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockResponse);

        String csvData = dbImportServiceImpl.getTableDataAsCSV(tableName);
        String expectedCsv = "sk,pk\r\n1,1\r\n2,2\r\n";
        assertEquals(expectedCsv, csvData);
    }

    @Test
    void testClearTableData() throws Exception {
        String tableName = "Skill Lookups Master Table";

        // Mocking the items in the table
        List<Map<String, AttributeValue>> items = new ArrayList<>();
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("pk", AttributeValue.builder().s("1").build());
        item1.put("sk", AttributeValue.builder().s("1").build());
        items.add(item1);

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("pk", AttributeValue.builder().s("2").build());
        item2.put("sk", AttributeValue.builder().s("2").build());
        items.add(item2);

        // Mocking the scan response
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(items)
            .lastEvaluatedKey(null) // No more pages
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        // Mocking the delete response
        DeleteItemResponse mockDeleteResponse = DeleteItemResponse.builder().build();
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(mockDeleteResponse);

        // Call the method
        boolean result = dbImportServiceImpl.clearTableData(tableName);

        // Assertions
        assertEquals(true, result);
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
        verify(dynamoDbClient, times(1)).batchWriteItem(any(BatchWriteItemRequest.class));

    }
    @Test
    void testDeleteRequestsBatchProcessing() throws Exception {
        String tableName = "Skill Lookups Master Table";

        // Mocking 30 items in the table
        List<Map<String, AttributeValue>> items = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.builder().s(String.valueOf(i)).build());
            item.put("sk", AttributeValue.builder().s(String.valueOf(i)).build());
            items.add(item);
        }

        // Mocking the scan response
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(items)
            .lastEvaluatedKey(null) // No more pages
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        // Mocking the batch write response
        BatchWriteItemResponse mockBatchWriteResponse = BatchWriteItemResponse.builder()
            .unprocessedItems(Collections.emptyMap())
            .build();
        when(dynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class))).thenReturn(mockBatchWriteResponse);

        // Call the method
        boolean result = dbImportServiceImpl.clearTableData(tableName);

        // Assertions
        assertEquals(true, result);
        ArgumentCaptor<BatchWriteItemRequest> captor = ArgumentCaptor.forClass(BatchWriteItemRequest.class);
        verify(dynamoDbClient, times(2)).batchWriteItem(captor.capture());

        // Verify the first batch contains 25 items
        List<BatchWriteItemRequest> capturedRequests = captor.getAllValues();
        assertEquals(25, capturedRequests.get(0).requestItems().get(skillLookupsTable).size());

        // Verify the second batch contains the remaining 5 items
        assertEquals(5, capturedRequests.get(1).requestItems().get(skillLookupsTable).size());
    }

    @Test
    void testUpdateUserCountry_Success() {
        String tableName = "Skill Lookups Master Table";

        // Mock ScanResponse with items
        Map<String, AttributeValue> item1 = Map.of(
                "pk", AttributeValue.builder().s("user1").build(),
                "sk", AttributeValue.builder().s("meta1").build(),
                "country", AttributeValue.builder().s("").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
                "pk", AttributeValue.builder().s("user2").build(),
                "sk", AttributeValue.builder().s("meta2").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
                .items(Arrays.asList(item1, item2))
                .lastEvaluatedKey(null)
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        BatchWriteItemResponse mockBatchWriteResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(Collections.emptyMap())
                .build();
        when(dynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class))).thenReturn(mockBatchWriteResponse);

        int updatedRecords = dbImportServiceImpl.updateUserCountry(tableName);

        assertEquals(2, updatedRecords);
    }

    @Test
    void testUpdateUserCountry_NoRecordsToUpdate() {
        String tableName = "Skill Lookups Master Table";

        // Mock ScanResponse with no items
        ScanResponse mockScanResponse = ScanResponse.builder()
                .items(Collections.emptyList())
                .lastEvaluatedKey(null)
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        int updatedRecords = dbImportServiceImpl.updateUserCountry(tableName);

        assertEquals(0, updatedRecords);
    }

    @Test
    void testUpdateUserCountry_UnprocessedItems() {
        String tableName = "Skill Lookups Master Table";

        // Mock ScanResponse with items
        Map<String, AttributeValue> item1 = Map.of(
                "pk", AttributeValue.builder().s("user1").build(),
                "sk", AttributeValue.builder().s("meta1").build(),
                "country", AttributeValue.builder().s("").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
                .items(Collections.singletonList(item1))
                .lastEvaluatedKey(null)
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        // Mock BatchWriteItemResponse with unprocessed items
        Map<String, List<WriteRequest>> unprocessedItems = Map.of(
                tableName, Collections.singletonList(
                        WriteRequest.builder()
                                .putRequest(PutRequest.builder().item(item1).build())
                                .build()
                )
        );
        BatchWriteItemResponse mockBatchWriteResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(unprocessedItems)
                .build();
        when(dynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class))).thenReturn(mockBatchWriteResponse);

        int updatedRecords = dbImportServiceImpl.updateUserCountry(tableName);

        assertEquals(0, updatedRecords);
    }

    @Test
    void testGetTableDisplayNames() {
        List<TableDisplayNameDto> displayNames = dbImportServiceImpl.getTableDisplayNames();
        List<String> expectedNames = Arrays.asList("Skill Lookups Master Table", "Lookup Master Table", "Roles Master Table");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals("master", displayNames.get(i).getTableType());
            assertEquals(expectedNames.get(i), displayNames.get(i).getName());
        }
    }
}

