package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.TableDisplayNameDto;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.service.DBImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

public class DBImportControllerTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @Mock
    private MultipartFile file;

    @Mock
    private DBImportServiceImpl dbImportService;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private DBImportController dbImportController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dynamoDbClient = mock(DynamoDbClient.class);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient); // Mock the DynamoDB client from the config

    }

    @Test
    public void testDbImport_Success() throws Exception {
        String tableName = "testTable";
        ResponseEntity<HttpResponse> response = dbImportController.dbImport(file, tableName);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("0", response.getBody().getData().toString());
    }


    @Test
    public void testDbImport_TableNameIsNull() throws Exception {

        ResponseEntity<HttpResponse> response = dbImportController.dbImport(file, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Table name is required", response.getBody().getData().toString());


    }

    @Test
    public void testDbImport_TableNameIsEmpty() throws Exception {
        ResponseEntity<HttpResponse> response = dbImportController.dbImport(file, "");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Table name is required", response.getBody().getData().toString());
    }

//  @Test
//  public void testClearTableData_ForbiddenEnvironment() {
//    String tableName = "testTable";
//    System.setProperty("APP_ENV", "dev"); // Set environment to forbidden value
//    System.out.println("APP_ENV: " + System.getProperty("APP_ENV")); // Debug log
//    ResponseEntity<Void> response = dbImportController.clearTableData(tableName);
//    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//  }
//
//
//  @Test
//  public void testClearTableData_Success() throws Exception {
//    String tableName = "testTable";
//    System.setProperty("APP_ENV", "prod"); // Set environment variable directly
//    when(dbImportService.clearTableData(tableName)).thenReturn(true);
//
//    ResponseEntity<Void> response = dbImportController.clearTableData(tableName);
//
//    assertEquals(HttpStatus.OK, response.getStatusCode());
//  }
//
//
//    @Test
//    public void testClearTableData_Failure() throws Exception {
//        String tableName = "testTable";
//        System.setProperty("APP_ENV", "sbx"); // Set environment to allowed value
//        when(dbImportService.clearTableData(tableName)).thenReturn(false);
//
//        ResponseEntity<Void> response = dbImportController.clearTableData(tableName);
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    @Test
//    public void testClearTableData_Exception() throws Exception {
//        String tableName = "testTable";
//        System.setProperty("APP_ENV", "sbx"); // Set environment to allowed value
//        when(dbImportService.clearTableData(tableName)).thenThrow(new RuntimeException("Error"));
//
//        ResponseEntity<Void> response = dbImportController.clearTableData(tableName);
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }

    @Test
    public void testDownloadTableAsCSV_Success() throws Exception {
        String tableName = "testTable";
        String csvData = "id,name\n1,John\n2,Jane";
        ScanResponse mockScanResponse = ScanResponse.builder().build(); // Define mockScanResponse
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);
        when(dbImportService.getTableDataAsCSV(tableName)).thenReturn(csvData);

        ResponseEntity<String> response = dbImportController.downloadTableAsCSV(tableName);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=" + tableName + ".csv", response.getHeaders().getFirst("Content-Disposition"));
    }

//  @Test
//  public void testDownloadTableAsCSV_InternalServerError() throws Exception {
//    String tableName = "testTable";
//    ScanResponse mockScanResponse = ScanResponse.builder().build(); // Define mockScanResponse
//    when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);
//    when(dbImportService.getTableDataAsCSV(any(String.class))).thenThrow(new IOException());
//
//    ResponseEntity<String> response = dbImportController.downloadTableAsCSV(tableName);
//
//    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    assertNull(response.getBody());
//  }

    @Test
    public void testUpdateUserCountry_TableNameIsNull() throws Exception {
        ResponseEntity<HttpResponse> response = dbImportController.updateUserCountry(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Table name is required", response.getBody().getData());
        assertEquals(null, response.getBody().getError());
    }

    @Test
    public void testUpdateUserCountry_TableNameIsEmpty() throws Exception {
        ResponseEntity<HttpResponse> response = dbImportController.updateUserCountry("");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Table name is required", response.getBody().getData());
        assertEquals(null, response.getBody().getError());
    }

    @Test
    public void testGetTableDisplayNames_Success() {
        List<TableDisplayNameDto> mockDisplayNames = Arrays.asList(
                new TableDisplayNameDto("clear", "Course Enrollment Table")
        );
        when(dbImportService.getTableDisplayNames()).thenReturn(mockDisplayNames);

        ResponseEntity<HttpResponse> response = dbImportController.getTableDisplayNames();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockDisplayNames, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    public void testGetTableDisplayNames_Exception() {
        when(dbImportService.getTableDisplayNames()).thenThrow(new RuntimeException("Error fetching display names"));
        ResponseEntity<HttpResponse> response = dbImportController.getTableDisplayNames();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getData());
        assertEquals("Error fetching display names", response.getBody().getError());
    }
}