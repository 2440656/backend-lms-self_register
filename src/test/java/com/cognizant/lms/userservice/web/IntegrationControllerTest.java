package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationSummaryResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.SFTPConnectionException;
import com.cognizant.lms.userservice.service.IntegrationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class IntegrationControllerTest {

  @Mock
  private IntegrationService integrationService;

  @InjectMocks
  private IntegrationController integrationController;

  public IntegrationControllerTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSaveIntegration_Success() {
    IntegrationDraftRequest request = new IntegrationDraftRequest();
    IntegrationDto integrationDto = new IntegrationDto();
    HttpResponse expectedResponse = new HttpResponse();
    expectedResponse.setData(integrationDto);
    expectedResponse.setStatus(CREATED.value());

    when(integrationService.saveIntegration(request)).thenReturn(integrationDto);

    ResponseEntity<HttpResponse> response = integrationController.saveIntegration(request);

    assertEquals(CREATED, response.getStatusCode());
    assertEquals(expectedResponse.getStatus(), response.getBody().getStatus());
    assertEquals(expectedResponse.getData(), response.getBody().getData());
    verify(integrationService, times(1)).saveIntegration(request);
  }

  @Test
  void testSaveIntegration_Exception() {
    IntegrationDraftRequest request = new IntegrationDraftRequest();
    when(integrationService.saveIntegration(request)).thenThrow(new RuntimeException("Error"));

    ResponseEntity<HttpResponse> response = integrationController.saveIntegration(request);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).saveIntegration(request);
  }


  @Test
  void getIntegrationByLmsIntegrationId_found() {
    String lmsIntegrationId = "id1";
    String pageName = "GeneralInformation";
    IntegrationDto integrationDto = new IntegrationDto();
    when(integrationService.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName)).thenReturn(integrationDto);

    ResponseEntity<HttpResponse> response = integrationController.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);

    assertEquals(OK, response.getStatusCode());
    assertEquals(OK.value(), response.getBody().getStatus());
    assertEquals(integrationDto, response.getBody().getData());
    assertNull(response.getBody().getError());
    verify(integrationService, times(1)).getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);
  }

  @Test
  void getIntegrationByLmsIntegrationId_notFound() {
    String lmsIntegrationId = "id1";
    String pageName = "GeneralInformation";
    when(integrationService.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName)).thenReturn(null);

    ResponseEntity<HttpResponse> response = integrationController.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);

    assertEquals(NOT_FOUND, response.getStatusCode());
    assertEquals(NOT_FOUND.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);
  }

  @Test
  void getIntegrationByLmsIntegrationId_exception() {
    String lmsIntegrationId = "id1";
    String pageName = "GeneralInformation";
    when(integrationService.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName)).thenThrow(new RuntimeException("fail"));

    ResponseEntity<HttpResponse> response = integrationController.getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).getIntegrationByLmsIntegrationId(lmsIntegrationId, pageName);
  }

  @Test
  void testGetIntegrationSummary_Success() {
    IntegrationSummaryResponse summaryResponse = new IntegrationSummaryResponse();
    summaryResponse.setStatus(200); // Set necessary fields for the response
    when(integrationService.getIntegrationSummary(
        anyString(), anyString(), any(), anyInt(), any(), any(), any()))
        .thenReturn(summaryResponse);

    ResponseEntity<IntegrationSummaryResponse> response = integrationController.getIntegrationSummary(
        "createdOn", "desc", null, 10, null, null, null);

    assertEquals(OK, response.getStatusCode());
    assertEquals(summaryResponse, response.getBody());
    verify(integrationService, times(1)).getIntegrationSummary(
        anyString(), anyString(), any(), anyInt(), any(), any(), any());
  }

  @Test
  void testGetIntegrationSummary_Exception() {
    when(integrationService.getIntegrationSummary(
        anyString(), anyString(), any(), anyInt(), any(), any(), any()))
        .thenThrow(new RuntimeException("Error"));

    ResponseEntity<IntegrationSummaryResponse> response = integrationController.getIntegrationSummary(
        "createdOn", "desc", null, 10, null, null, null);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).getIntegrationSummary(
        anyString(), anyString(), any(), anyInt(), any(), any(), any());
  }

  @Test
  void testSaveSFTPIntegration_Success() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    SFTPIntegrationResponseDto responseDto = new SFTPIntegrationResponseDto();
    HttpResponse expectedResponse = new HttpResponse();
    expectedResponse.setData(responseDto);
    expectedResponse.setStatus(CREATED.value());

    when(integrationService.saveSFTPIntegration(request)).thenReturn(responseDto);

    ResponseEntity<HttpResponse> response = integrationController.saveSFTPIntegration(request);

    assertEquals(CREATED, response.getStatusCode());
    assertEquals(expectedResponse.getStatus(), response.getBody().getStatus());
    assertEquals(expectedResponse.getData(), response.getBody().getData());
    verify(integrationService, times(1)).saveSFTPIntegration(request);
  }

  @Test
  void testSaveSFTPIntegration_InvalidInput() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();

    when(integrationService.saveSFTPIntegration(request)).thenThrow(new IllegalArgumentException("Invalid input"));

    ResponseEntity<HttpResponse> response = integrationController.saveSFTPIntegration(request);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).saveSFTPIntegration(request);
  }

  @Test
  void testSaveSFTPIntegration_Exception() {
    SFTPIntegrationReqDto request = new SFTPIntegrationReqDto();
    when(integrationService.saveSFTPIntegration(request)).thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<HttpResponse> response = integrationController.saveSFTPIntegration(request);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    verify(integrationService, times(1)).saveSFTPIntegration(request);
  }

  // Test case for successful retrieval of SFTP integration
  @Test
  void testGetSftpIntegrationByLmsIntegrationId_Success() {
    String lmsIntegrationId = "testId";
    String pageName = "SFTPConfiguration";
    SFTPIntegrationResponseDto responseDto = new SFTPIntegrationResponseDto();
    responseDto.setLmsIntegrationId(lmsIntegrationId);

    when(integrationService.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false)).thenReturn(responseDto);

    ResponseEntity<HttpResponse> response = integrationController.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
    assertEquals(responseDto, response.getBody().getData());
    assertNull(response.getBody().getError());
    verify(integrationService, times(1))
        .getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);
  }

  // Test case for SFTP integration not found
  @Test
  void testGetSftpIntegrationByLmsIntegrationId_NotFound() {
    String lmsIntegrationId = "testId";
    String pageName = "SFTPConfiguration";
    when(integrationService.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false))
        .thenReturn(null);
    ResponseEntity<HttpResponse> response =
        integrationController.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    assertEquals("sftp Integration not found with lmsIntegrationId: "
        + lmsIntegrationId, response.getBody().getError());
    verify(integrationService, times(1))
        .getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);
  }

  @Test
  void testGetSftpIntegrationByLmsIntegrationId_Exception() {
    String lmsIntegrationId = "testId";
    String pageName = "SFTPConfiguration";
    when(integrationService.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false))
        .thenThrow(new RuntimeException("Unexpected error"));
    ResponseEntity<HttpResponse> response =
        integrationController.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertNotNull(response.getBody().getError());
    assertEquals("An unexpected error occurred : Unexpected error", response.getBody().getError());
    verify(integrationService, times(1))
        .getSftpIntegrationByLmsIntegrationId(lmsIntegrationId, pageName, false);
  }
  @Test
  void testSftpTestConnection_Success() throws IOException {
    String sftpUserName = "testUser";
    String sftpPassword = "testPassword";
    String sftpLocation = "/test/location";
    String sftpPort = "22";
    String sftpHost = "testHost";
    String expectedResponse = "Connection Successful";
    HttpResponse expectedHttpResponse = new HttpResponse();
    expectedHttpResponse.setData(expectedResponse);
    expectedHttpResponse.setStatus(HttpStatus.OK.value());
    expectedHttpResponse.setError(null);
    when(integrationService.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost))
        .thenReturn(expectedResponse);
    ResponseEntity<HttpResponse> response = integrationController.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedHttpResponse.getStatus(), response.getBody().getStatus());
    assertEquals(expectedHttpResponse.getData(), response.getBody().getData());
    assertNull(response.getBody().getError());
    verify(integrationService, times(1))
        .sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
  }

  @Test
  void testSftpTestConnection_SFTPConnectionException() throws IOException {
    String sftpUserName = "testUser";
    String sftpPassword = "testPassword";
    String sftpLocation = "/test/location";
    String sftpPort = "22";
    String sftpHost = "testHost";
    String errorMessage = "SFTP connection failed";
    when(integrationService.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost))
        .thenThrow(new SFTPConnectionException(errorMessage));
    ResponseEntity<HttpResponse> response =
        integrationController.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    assertEquals(errorMessage, response.getBody().getError());
    verify(integrationService, times(1)).sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
  }

  @Test
  void testSftpTestConnection_Exception() throws IOException {
    String sftpUserName = "testUser";
    String sftpPassword = "testPassword";
    String sftpLocation = "/test/location";
    String sftpPort = "22";
    String sftpHost = "testHost";
    when(integrationService.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost))
        .thenThrow(new RuntimeException("Unexpected error"));
    ResponseEntity<HttpResponse> response = integrationController.sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    assertEquals("Unexpected error occurred", response.getBody().getError());
    verify(integrationService, times(1))
        .sftpTestConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
  }
}
