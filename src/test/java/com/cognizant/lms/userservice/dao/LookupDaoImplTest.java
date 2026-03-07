package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LookupDaoImplTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    private LookupDaoImpl countryDao;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        countryDao = new LookupDaoImpl(dynamoDBConfig, "CountryTable");
    }

    @Test
    public void testGetAllCountries_ReturnsCountryList() {
        // Arrange
        Map<String, AttributeValue> item1 = Map.of(
                "pk", AttributeValue.builder().s("LOOKUP#Country").build(),
                "sk", AttributeValue.builder().s("COUNTRY#ABD").build(),
                "name", AttributeValue.builder().s("Abu Dhabi").build()
        );

        Map<String, AttributeValue> item2 = Map.of(
                "pk", AttributeValue.builder().s("LOOKUP#Country").build(),
                "sk", AttributeValue.builder().s("COUNTRY#AFG").build(),
                "name", AttributeValue.builder().s("Afghanistan").build()
        );

        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of(item1, item2))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        List<LookupDto> countries = countryDao.getLookupData("Country", null);

        // Assert
        assertEquals(2, countries.size());
        assertEquals("Abu Dhabi", countries.get(0).getName());
        assertEquals("Afghanistan", countries.get(1).getName());
    }

    @Test
    public void testGetAllCountries_HandlesEmptyResponse() {
        // Arrange
        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        List<LookupDto> countries = countryDao.getLookupData("Country", null);

        // Assert
        assertEquals(0, countries.size());
    }

    @Test
    public void testGetAllCountries_HandlesDynamoDbException() {
        // Arrange
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        // Act
        List<LookupDto> countries = countryDao.getLookupData("Country", null);

        // Assert
        assertEquals(0, countries.size());
    }

    @Test
    void testGetServiceLineLookupData_ReturnsList() {
        try (MockedStatic<TenantUtil> tenantUtil = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn("TENANT1");

            Map<String, AttributeValue> item1 = Map.of(
                    "pk", AttributeValue.builder().s("TENANT1#ServiceLine").build(),
                    "sk", AttributeValue.builder().s("SERVICELINE#001").build(),
                    "name", AttributeValue.builder().s("Service Line 1").build()
            );
            Map<String, AttributeValue> item2 = Map.of(
                    "pk", AttributeValue.builder().s("TENANT1#ServiceLine").build(),
                    "sk", AttributeValue.builder().s("SERVICELINE#002").build(),
                    "name", AttributeValue.builder().s("Service Line 2").build()
            );
            QueryResponse response = QueryResponse.builder().items(List.of(item1, item2)).build();
            when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

            List<LookupDto> result = countryDao.getServiceLineLookupData("ServiceLine", null);

            assertEquals(2, result.size());
            assertEquals("Service Line 1", result.get(0).getName());
            assertEquals("Service Line 2", result.get(1).getName());
        }
    }

    @Test
    void testGetServiceLineLookupData_EmptyResponse() {
        try (MockedStatic<TenantUtil> tenantUtil = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn("TENANT1");
            QueryResponse response = QueryResponse.builder().items(List.of()).build();
            when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

            List<LookupDto> result = countryDao.getServiceLineLookupData("ServiceLine", null);

            assertEquals(0, result.size());
        }
    }

    @Test
    void testGetServiceLineLookupData_DynamoDbException() {
        try (MockedStatic<TenantUtil> tenantUtil = Mockito.mockStatic(TenantUtil.class)) {
            tenantUtil.when(TenantUtil::getTenantCode).thenReturn("TENANT1");
            when(dynamoDbClient.query(any(QueryRequest.class)))
                    .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

            List<LookupDto> result = countryDao.getServiceLineLookupData("ServiceLine", null);

            assertEquals(0, result.size());
        }
    }

    @Test
    public void testGetAiVoicePreviewData_ReturnsList() {
        Map<String, AttributeValue> item1 = Map.of(
                "pk", AttributeValue.builder().s("t-2#voice").build(),
                "sk", AttributeValue.builder().s("VOICE#001").build(),
                "fileName", AttributeValue.builder().s("file1.mp3").build(),
                "gender", AttributeValue.builder().s("male").build(),
                "language", AttributeValue.builder().s("en").build(),
                "path", AttributeValue.builder().s("/voices/file1.mp3").build(),
                "voiceId", AttributeValue.builder().s("v1").build(),
                "voiceName", AttributeValue.builder().s("Voice One").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
                "pk", AttributeValue.builder().s("t-2#voice").build(),
                "sk", AttributeValue.builder().s("VOICE#002").build(),
                "fileName", AttributeValue.builder().s("file2.mp3").build(),
                "gender", AttributeValue.builder().s("female").build(),
                "language", AttributeValue.builder().s("fr").build(),
                "path", AttributeValue.builder().s("/voices/file2.mp3").build(),
                "voiceId", AttributeValue.builder().s("v2").build(),
                "voiceName", AttributeValue.builder().s("Voice Two").build()
        );
        QueryResponse response = QueryResponse.builder().items(List.of(item1, item2)).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<AiVoicePreviewLookupDto> result = countryDao.getAiVoicePreviewData();

        assertEquals(2, result.size());
        assertEquals("file1.mp3", result.get(0).getFileName());
        assertEquals("file2.mp3", result.get(1).getFileName());
    }

    @Test
    public void testGetAiVoicePreviewData_EmptyResponse() {
        QueryResponse response = QueryResponse.builder().items(List.of()).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<AiVoicePreviewLookupDto> result = countryDao.getAiVoicePreviewData();

        assertEquals(0, result.size());
    }

    @Test
    public void testGetAiVoicePreviewData_DynamoDbException() {
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        List<AiVoicePreviewLookupDto> result = countryDao.getAiVoicePreviewData();

        assertEquals(0, result.size());
    }
}
