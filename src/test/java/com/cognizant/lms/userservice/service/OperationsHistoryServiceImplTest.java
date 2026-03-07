package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.OperationsHistoryFilterSortDao;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.dto.LogFileResponse;
import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@ExtendWith(SpringExtension.class)
class OperationsHistoryServiceImplTest {

  private OperationsHistoryServiceImpl operationsHistoryServiceImpl;
  private OperationsHistoryFilterSortDao operationsHistoryFilterSortDao;
  private Authentication authentication;
  private AuthUser authUser;
  private SecurityContext securityContext;

  @BeforeEach
  void init() {
    operationsHistoryFilterSortDao = mock(OperationsHistoryFilterSortDao.class);
    securityContext = mock(SecurityContext.class);
    authentication = mock(Authentication.class);
    authUser = mock(AuthUser.class);
    SecurityContextHolder.setContext(securityContext);
    operationsHistoryServiceImpl = new OperationsHistoryServiceImpl(operationsHistoryFilterSortDao);
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
  }

  @Test
  void getLogFileDetailsFilterByProcess_Success() {
    List<OperationsHistory> errorLogs = new ArrayList<>();
    errorLogs.add(new OperationsHistory("uuid1", "lms-admin",
        "addUsers.csv", "jhon", "10/15/2024", "jhon@gmail.com",
        "t-2", "Add Users", "user man", null,"","", ""));
    errorLogs.add(new OperationsHistory("uuid2", "Add Users",
        "addUsers.csv", "jeo", "10/15/2024",
        "jeo@gmail.com", "t-2", "Add Users", "user man", null,"","", ""));
    String partitionKeyValue = "pkValue";
    String sortKey = "sort";
    String order = "any";
    String process = "some process";
    int perPage = 10;
    LogFileResponse logFileResponse = new LogFileResponse(errorLogs, null);

    when(operationsHistoryFilterSortDao.getLogFileLists(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyMap(), eq(perPage)))
        .thenReturn(logFileResponse);

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserEmail()).thenReturn("testEmail");

    Map<String, String> lastEvaluatedKey = new HashMap<>();
    LogFileResponse result = operationsHistoryServiceImpl
        .getLogFileDetailsFilterByProcess(partitionKeyValue, sortKey, order, process, lastEvaluatedKey, perPage);

    assertNotNull(result);
    assertEquals(2, (result.getLogFiles()).size());
    verify(operationsHistoryFilterSortDao)
        .getLogFileLists(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap(), eq(perPage));
    verify(operationsHistoryFilterSortDao, times(1))
        .getLogFileLists(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap(), eq(perPage));
  }

  @Test
  void testGetLogFileDetailsFilterByProcess_NoLogsFound() {
    String partitionKeyValue = "pkValue";
    String sortKey = "sort";
    String order = "any";
    String process = "some process";
    int perPage = 10;
    Map<String, String> lastEvaluatedKey = new HashMap<>();
    LogFileResponse response = new LogFileResponse();
    response.setLogFiles(new ArrayList<>());
    when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(),
        anyString(), anyString(), isNull(), anyMap(), eq(perPage))).thenReturn(response);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    LogFileResponse actualResponse = operationsHistoryServiceImpl
        .getLogFileDetailsFilterByProcess(partitionKeyValue, sortKey, order, process, lastEvaluatedKey, perPage);
    assertEquals(0, actualResponse.getLogFiles().size());
    verify(operationsHistoryFilterSortDao).getLogFileLists(anyString(), anyString(),
        anyString(), anyString(), isNull(), anyMap(), eq(perPage));
    verify(operationsHistoryFilterSortDao, times(1))
        .getLogFileLists(anyString(), anyString(), anyString(), anyString(), isNull(), anyMap(), eq(perPage));
  }

  @Test
  void testGetLogFileDetailsFilterByProcess_ExceptionHandling() {
    String partitionKeyValue = "pkValue";
    String sortKey = "sort";
    String order = "any";
    String process = "some process";
    int perPage = 10;
    Map<String, String> lastEvaluatedKey = new HashMap<>();
    when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(),
        anyString(), anyString(), anyString(), anyMap(), eq(perPage))).thenThrow(
        new RuntimeException("Database error"));
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      operationsHistoryServiceImpl
          .getLogFileDetailsFilterByProcess(partitionKeyValue, sortKey, order, process, lastEvaluatedKey, perPage);
    });
    assertEquals("Database error", exception.getMessage());
    verify(operationsHistoryFilterSortDao).getLogFileLists(anyString(), anyString(),
        anyString(), anyString(), anyString(), anyMap(), eq(perPage));
    verify(operationsHistoryFilterSortDao, times(1))
        .getLogFileLists(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap(), eq(perPage));
  }

  @Test
  void getLogFiles_withoutPassProcess_success() {
    List<OperationsHistory> errorLogs = new ArrayList<>();
    errorLogs.add(new OperationsHistory("uuid1", "lms-admin", "addUsers.csv",
        "jhon", "10/15/2024", "jhon@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    errorLogs.add(new OperationsHistory("uuid2", "Add Users", "addUsers.csv",
        "jeo", "10/15/2024", "jeo@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    LogFileResponse logFileResponse = new LogFileResponse();
    logFileResponse.setLogFiles(errorLogs);
    logFileResponse.setLastEvaluatedKey(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    String area = Constants.AREA_USER_MANAGEMENT;
    when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(), anyString(),
            any(), anyString(), any(), anyInt()))
            .thenReturn(logFileResponse);
    OperationHistoryResponse response = operationsHistoryServiceImpl.getLogFiles("sortKey",
        "desc", null, area, null, 10);
    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatus());
    assertEquals(2, ((List<?>) response.getData()).size());
    verify(operationsHistoryFilterSortDao, times(1))
            .getLogFileLists(anyString(), anyString(), anyString(), any(), anyString(), any(), anyInt());
  }

  @Test
  void getLogFiles_withPassProcess_success() {
    List<OperationsHistory> errorLogs = new ArrayList<>();
    errorLogs.add(new OperationsHistory("uuid1", "lms-admin", "addUsers.csv",
        "jhon", "10/15/2024", "jhon@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    errorLogs.add(new OperationsHistory("uuid2", "Add Users", "addUsers.csv",
        "jeo", "10/15/2024", "jeo@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    LogFileResponse logFileResponse = new LogFileResponse();
    logFileResponse.setLogFiles(errorLogs);
    logFileResponse.setLastEvaluatedKey(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    String area = Constants.AREA_USER_MANAGEMENT;
    String process = Constants.ACTION_ADD;
    when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(), anyString(),
            any(), anyString(), any(), anyInt()))
            .thenReturn(logFileResponse);
    OperationHistoryResponse response = operationsHistoryServiceImpl.getLogFiles("sortKey",
        "desc", process, area, null,10);
    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatus());
    assertEquals(2, ((List<?>) response.getData()).size());
    verify(operationsHistoryFilterSortDao, times(1))
            .getLogFileLists(anyString(), anyString(), anyString(), any(), anyString(), any(), anyInt());
  }

  @Test
  void getLogFiles_invalidBase64() {
    OperationHistoryResponse response = operationsHistoryServiceImpl
        .getLogFiles("sort", "any", "some process","some area", "invalidBase64", 10);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    assertEquals("Invalid Base64 encoded lastEvaluatedKey", response.getError());
  }

  @Test
  void getLogFiles_withLastEvaluatedKey_success() {
    List<OperationsHistory> errorLogs = new ArrayList<>();
    errorLogs.add(new OperationsHistory("uuid1", "lms-admin", "addUsers.csv",
        "jhon", "10/15/2024", "jhon@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    errorLogs.add(new OperationsHistory("uuid2", "Add Users", "addUsers.csv",
        "jeo", "10/15/2024", "jeo@gmail.com", "t-2",
        "Add Users", "user man", null,"","", ""));
    Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
    lastEvaluatedKey.put("key", AttributeValue.builder().s("value").build());
    LogFileResponse logFileResponse = new LogFileResponse();
    logFileResponse.setLogFiles(errorLogs);
    logFileResponse.setLastEvaluatedKey(lastEvaluatedKey);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    String area = Constants.AREA_USER_MANAGEMENT;
    when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(), anyString(),
            any(), anyString(), any(), anyInt()))
            .thenReturn(logFileResponse);
    OperationHistoryResponse response = operationsHistoryServiceImpl.getLogFiles("sortKey",
        "desc", null, area, null, 10);
    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatus());
    assertEquals(2, ((List<?>) response.getData()).size());
    assertNotNull(response.getLastEvaluatedKey());
    verify(operationsHistoryFilterSortDao, times(1))
            .getLogFileLists(anyString(), anyString(), anyString(), any(), anyString(), any(), anyInt());
  }

  @Test
  void getLogFiles_invalidProcess() {
    String invalidProcess = "invalidProcess";
    String area = Constants.AREA_USER_MANAGEMENT;
    OperationHistoryResponse response = operationsHistoryServiceImpl
        .getLogFiles("sortKey", "desc", invalidProcess, area,null, 10);
    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    assertNull(response.getData());
    assertEquals("Invalid process value: " + invalidProcess, response.getError());
  }

  @Test
  void testGetLogFiles_invalidArea() {
      String invalidArea = "invalidArea";
      OperationHistoryResponse response = operationsHistoryServiceImpl
          .getLogFiles("sortKey", "desc", null, invalidArea, null, 10);
      assertNotNull(response);
      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
      assertNull(response.getData());
      assertEquals("Invalid area value: " + invalidArea, response.getError());
  }

  @Test
  void getLogFiles_skillsManagement_withoutPassProcess_success() {
      List<OperationsHistory> errorLogs = new ArrayList<>();
      errorLogs.add(new OperationsHistory("uuid1", "lms-admin", "uploadSkills.csv",
              "jhon", "10/15/2024", "jhon@gmail.com", "t-2",
              "Upload Skills", Constants.AREA_SKILL_MANAGEMENT, null,"","", ""));
      errorLogs.add(new OperationsHistory("uuid2", "Upload Skills", "uploadSkills.csv",
              "jeo", "10/15/2024", "jeo@gmail.com", "t-2",
              "Upload Skills", Constants.AREA_SKILL_MANAGEMENT, null,"","", ""));
      LogFileResponse logFileResponse = new LogFileResponse();
      logFileResponse.setLogFiles(errorLogs);
      logFileResponse.setLastEvaluatedKey(null);
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(authUser);
      when(authUser.getUsername()).thenReturn("testUser");
      when(authUser.getUserEmail()).thenReturn("testEmail");
      String area = Constants.AREA_SKILL_MANAGEMENT;
      when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(), anyString(),
              any(), anyString(), any(), anyInt()))
              .thenReturn(logFileResponse);
      OperationHistoryResponse response = operationsHistoryServiceImpl.getLogFiles("sortKey",
              "desc", null, area, null, 10);
      assertNotNull(response);
      assertEquals(HttpStatus.OK.value(), response.getStatus());
      assertEquals(2, ((List<?>) response.getData()).size());
      verify(operationsHistoryFilterSortDao, times(1))
              .getLogFileLists(anyString(), anyString(), anyString(), any(), anyString(), any(), anyInt());
  }

  @Test
  void getLogFiles_skillsManagement_withPassProcess_success() {
      List<OperationsHistory> errorLogs = new ArrayList<>();
      errorLogs.add(new OperationsHistory("uuid1", "lms-admin", "uploadSkills.csv",
              "jhon", "10/15/2024", "jhon@gmail.com", "t-2",
              "Upload Skills", Constants.AREA_SKILL_MANAGEMENT, null,"","", ""));
      LogFileResponse logFileResponse = new LogFileResponse();
      logFileResponse.setLogFiles(errorLogs);
      logFileResponse.setLastEvaluatedKey(null);
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(authUser);
      when(authUser.getUsername()).thenReturn("testUser");
      when(authUser.getUserEmail()).thenReturn("testEmail");
      String area = Constants.AREA_SKILL_MANAGEMENT;
      String process = Constants.ACTION_UPLOAD_SKILLS;
      when(operationsHistoryFilterSortDao.getLogFileLists(anyString(), anyString(), anyString(),
              any(), anyString(), any(), anyInt()))
              .thenReturn(logFileResponse);
      OperationHistoryResponse response = operationsHistoryServiceImpl.getLogFiles("sortKey",
              "desc", process, area, null, 10);
      assertNotNull(response);
      assertEquals(HttpStatus.OK.value(), response.getStatus());
      assertEquals(1, ((List<?>) response.getData()).size());
      verify(operationsHistoryFilterSortDao, times(1))
              .getLogFileLists(anyString(), anyString(), anyString(), any(), anyString(), any(), anyInt());
  }

  @Test
  void getLogFiles_skillsManagement_invalidProcess() {
      String invalidProcess = "invalidProcess";
      String area = Constants.AREA_SKILL_MANAGEMENT;
      OperationHistoryResponse response = operationsHistoryServiceImpl
              .getLogFiles("sortKey", "desc", invalidProcess, area, null, 10);
      assertNotNull(response);
      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
      assertNull(response.getData());
      assertEquals("Invalid process value: " + invalidProcess, response.getError());
    }

}