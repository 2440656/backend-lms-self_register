package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocaleDataDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private DynamoDbClient dynamoDbClient;

    private LocaleDataDaoImpl localeDataDao;

    private final String tableName = "localeTable";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        localeDataDao = new LocaleDataDaoImpl(dynamoDBConfig, tableName);
    }

    @Test
    void testSaveLocaleData_callsPutItem() {
        LocaleRequestDTO dto = new LocaleRequestDTO("en", "home", "{\"k\":\"v\"}");
        PutItemResponse mockResponse = PutItemResponse.builder().build();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(mockResponse);

        localeDataDao.saveLocaleData(dto);

        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    void testGetLocaleDataForList_returnsMappedList() {
        Map<String, AttributeValue> item1 = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s("home").build(),
            "localeData", AttributeValue.builder().s("data1").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
            "languageCode", AttributeValue.builder().s("fr").build(),
            "pageName", AttributeValue.builder().s("about").build(),
            "localeData", AttributeValue.builder().s("data2").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item1, item2))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataForList();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("en", results.get(0).getLanguageCode());
        assertEquals("home", results.get(0).getPageName());
        assertEquals("data1", results.get(0).getLocaleData());
    }

    @Test
    void testGetLocaleDataByLanguageCode_queriesAndMaps() {
        String languageCode = "en";
        Map<String, AttributeValue> item = Map.of(
            "languageCode", AttributeValue.builder().s(languageCode).build(),
            "pageName", AttributeValue.builder().s("home").build(),
            "localeData", AttributeValue.builder().s("data1").build()
        );

        QueryResponse mockQueryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockQueryResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByLanguageCode(languageCode);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(languageCode, results.get(0).getLanguageCode());
        assertEquals("home", results.get(0).getPageName());
    }

    @Test
    void testGetLocaleDataByPageName_returnsMultipleLocales() {
        String pageName = "Dashboard";
        Map<String, AttributeValue> item1 = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("{\"greeting\": \"Hello\"}").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
            "languageCode", AttributeValue.builder().s("fr").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("{\"greeting\": \"Bonjour\"}").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item1, item2))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Dashboard", results.get(0).getPageName());
        assertEquals("en", results.get(0).getLanguageCode());
        assertEquals("Dashboard", results.get(1).getPageName());
        assertEquals("fr", results.get(1).getLanguageCode());
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    void testGetLocaleDataByPageName_returnsSingleLocale() {
        String pageName = "Login";
        Map<String, AttributeValue> item = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("{\"title\": \"Login Page\"}").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Login", results.get(0).getPageName());
        assertEquals("en", results.get(0).getLanguageCode());
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    void testGetLocaleDataByPageName_returnsEmptyListWhenNoItemsFound() {
        String pageName = "NonExistentPage";
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of())
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    void testGetLocaleDataByPageName_handlesNullItems() {
        String pageName = "TestPage";
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items((List<Map<String, AttributeValue>>) null)
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetLocaleDataByPageName_handlesPagination() {
        String pageName = "Dashboard";
        Map<String, AttributeValue> item1 = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("data1").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
            "languageCode", AttributeValue.builder().s("fr").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("data2").build()
        );

        Map<String, AttributeValue> lastKey = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build()
        );

        ScanResponse firstResponse = ScanResponse.builder()
            .items(List.of(item1))
            .lastEvaluatedKey(lastKey)
            .build();

        ScanResponse secondResponse = ScanResponse.builder()
            .items(List.of(item2))
            .build();

        when(dynamoDbClient.scan(any(ScanRequest.class)))
            .thenReturn(firstResponse)
            .thenReturn(secondResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(dynamoDbClient, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    void testGetLocaleDataByPageName_handlesNullLocaleData() {
        String pageName = "Settings";
        Map<String, AttributeValue> item = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Settings", results.get(0).getPageName());
        assertNull(results.get(0).getLocaleData());
    }

    @Test
    void testGetLocaleDataByPageName_handlesSpecialCharactersInPageName() {
        String pageName = "Common-Main";
        Map<String, AttributeValue> item = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s("{\"data\": \"test\"}").build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Common-Main", results.get(0).getPageName());
    }

    @Test
    void testGetLocaleDataByPageName_throwsDynamoDbException() {
        String pageName = "Dashboard";
        when(dynamoDbClient.scan(any(ScanRequest.class)))
            .thenThrow(ResourceNotFoundException.builder()
                .message("Table not found")
                .build());

        assertThrows(ResourceNotFoundException.class, () ->
            localeDataDao.getLocaleDataByPageName(pageName)
        );

        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    void testGetLocaleDataByPageName_verifiesScanRequestParameters() {
        String pageName = "TestPage";
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of())
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        localeDataDao.getLocaleDataByPageName(pageName);

        ArgumentCaptor<ScanRequest> requestCaptor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(dynamoDbClient).scan(requestCaptor.capture());

        ScanRequest capturedRequest = requestCaptor.getValue();
        assertEquals(tableName, capturedRequest.tableName());
        assertEquals("pageName = :pn", capturedRequest.filterExpression());
        assertTrue(capturedRequest.expressionAttributeValues().containsKey(":pn"));
        assertEquals(pageName, capturedRequest.expressionAttributeValues().get(":pn").s());
    }

    @Test
    void testGetLocaleDataByPageName_handlesComplexJsonData() {
        String pageName = "Dashboard";
        String complexJson = "{\"greeting\":{\"morning\":\"Good morning\",\"evening\":\"Good evening\"},\"actions\":[\"view\",\"edit\",\"delete\"]}";
        Map<String, AttributeValue> item = Map.of(
            "languageCode", AttributeValue.builder().s("en").build(),
            "pageName", AttributeValue.builder().s(pageName).build(),
            "localeData", AttributeValue.builder().s(complexJson).build()
        );

        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(List.of(item))
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);

        List<LocaleRequestDTO> results = localeDataDao.getLocaleDataByPageName(pageName);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(complexJson, results.get(0).getLocaleData());
    }

    @Test
    void testDeleteLocaleData_callsDeleteItem() {
        doReturn(DeleteItemResponse.builder().build()).when(dynamoDbClient).deleteItem(any(DeleteItemRequest.class));

        localeDataDao.deleteLocaleData("en", "home");

        verify(dynamoDbClient, times(1)).deleteItem(any(DeleteItemRequest.class));
    }
}

