package com.cognizant.lms.userservice.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import com.cognizant.lms.userservice.dto.SkillMigrationEventDetailDto;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.service.OperationsHistoryService;
import com.cognizant.lms.userservice.service.SkillLookupsService;
import com.cognizant.lms.userservice.service.UserManagementEventPublisherService;
import com.cognizant.lms.userservice.utils.TenantUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class SkillLookupsControllerTest {

  @Mock
  private SkillLookupsService skillLookupsService;

  @Mock
  private OperationsHistoryService operationsHistoryService;
  @Mock
  private UserManagementEventPublisherService userManagementEventPublisherService;

  @InjectMocks
  private SkillLookupsController skillLookupsController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
  }

  @Test
  void getSkillsLookupsByTypeNameOrCode_withSkillAndSearch_returnsOk() {
    String type = "Skill";
    String search = "Java";
    List<SkillLookups> skills = List.of(new SkillLookups());
    SkillLookupResponse mockResponse = new SkillLookupResponse(skills, null, 200, null);
    when(skillLookupsService.getSkillsLookupsByTypeNameOrCode(type, search)).thenReturn(mockResponse);

    ResponseEntity<SkillLookupResponse> response = skillLookupsController.getSkillsLookupsByTypeNameOrCode(type, search);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(200, response.getBody().getStatus());
    assertEquals(skills, response.getBody().getSkills());
  }

  @Test
  void getSkillsLookupsByTypeNameOrCode_withSkillAndEmptySearch_returnsBadRequest() {
    String type = "Skill";

    ResponseEntity<SkillLookupResponse> response = skillLookupsController.getSkillsLookupsByTypeNameOrCode(type, "");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getSkillsLookupsByTypeNameOrCode_withNonSkillAndNoSearch_returnsOk() {
    String type = "Skill-Cat";
    List<SkillLookups> skills = List.of(new SkillLookups());
    SkillLookupResponse mockResponse = new SkillLookupResponse(skills, null, 200, null);
    when(skillLookupsService.getSkillsLookupsByTypeNameOrCode(type, null)).thenReturn(mockResponse);

    ResponseEntity<SkillLookupResponse> response = skillLookupsController.getSkillsLookupsByTypeNameOrCode(type, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(skills, response.getBody().getSkills());
  }

  @Test
  void backfillSkillLookups_success_triggersEvent() {
    ResponseEntity<HttpResponse> response = skillLookupsController.backfillSkillLookups("abc");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ArgumentCaptor<SkillMigrationEventDetailDto> captor = ArgumentCaptor.forClass(SkillMigrationEventDetailDto.class);
    verify(userManagementEventPublisherService).triggerSkillMigrationEvent(captor.capture());
    SkillMigrationEventDetailDto sent = captor.getValue();
    assertEquals("abc", sent.getLastEvaluatedKey());
    assertEquals("t-2", sent.getTenantCode());
  }

  @Test
  void backfillSkillLookups_whenPublisherThrows_returnsServerError() {
    RuntimeException failure = new RuntimeException("boom");
    doThrow(failure).when(userManagementEventPublisherService).triggerSkillMigrationEvent(any());

    ResponseEntity<HttpResponse> response = skillLookupsController.backfillSkillLookups("abc");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
  @Test
  void getSkillCategoryBySkillName_withEmptySkillName_returnsBadRequestResponse() {
    String skillName = "";
    HttpResponse expectedResponse = new HttpResponse();
    expectedResponse.setData(null);
    expectedResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    expectedResponse.setError("Skill Id cannot be empty");
    ResponseEntity<HttpResponse> responseEntity = skillLookupsController.getSkillCategoryBySkillName(skillName);
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(expectedResponse.getStatus(), responseEntity.getBody().getStatus());
    assertEquals(expectedResponse.getData(), responseEntity.getBody().getData());
    assertEquals(expectedResponse.getError(), responseEntity.getBody().getError());
  }

    @Test
    void uploadSkillsCsv_success() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        FileUploadResponse fileUploadResponse = new FileUploadResponse();
        when(skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS")).thenReturn(fileUploadResponse);

        ResponseEntity<?> response = skillLookupsController.uploadSkillsCsv(mockFile, "UPLOAD_SKILLS");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(fileUploadResponse, ((com.cognizant.lms.userservice.dto.HttpResponse)response.getBody()).getData());
    }

    @Test
    void uploadSkillsCsv_illegalArgumentException_returnsBadRequest() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS")).thenThrow(new IllegalArgumentException("bad input"));

        ResponseEntity<?> response = skillLookupsController.uploadSkillsCsv(mockFile, "UPLOAD_SKILLS");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadSkillsCsv_exception_returnsBadRequestError() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = skillLookupsController.uploadSkillsCsv(mockFile, "UPLOAD_SKILLS");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void downloadSkillsFile_success() throws Exception {
        String filename = "errorlog.txt";
        String fileType = "txt";
        Resource resource = new ByteArrayResource("test".getBytes(StandardCharsets.UTF_8));
        when(skillLookupsService.getDownloadErrorLogFileForSkills(filename, fileType)).thenReturn(resource);

        ResponseEntity<Resource> response = skillLookupsController.downloadSkillsFile(filename, fileType);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resource, response.getBody());
    }

    @Test
    void downloadSkillsFile_fileNotFound_returnsNoContent() throws Exception {
        String filename = "missing.txt";
        String fileType = "txt";
        when(skillLookupsService.getDownloadErrorLogFileForSkills(filename, fileType)).thenThrow(new java.io.FileNotFoundException());

        ResponseEntity<Resource> response = skillLookupsController.downloadSkillsFile(filename, fileType);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void downloadSkillsFile_invalidFileName_returnsBadRequest() {
        ResponseEntity<Resource> response = skillLookupsController.downloadSkillsFile("..", "txt");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void fetchSkillsImportHistory_success() {
        OperationHistoryResponse mockResponse = new OperationHistoryResponse();
        mockResponse.setStatus(HttpStatus.OK.value());
        when(operationsHistoryService.getLogFiles(anyString(), anyString(), any(), anyString(), any(), anyInt()))
                .thenReturn(mockResponse);

        ResponseEntity<OperationHistoryResponse> response = skillLookupsController.fetchSkillsImportHistory("createdOn", "desc", null, "skill-management", null, 5);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void fetchSkillsImportHistory_badRequest() {
        OperationHistoryResponse mockResponse = new OperationHistoryResponse();
        mockResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        when(operationsHistoryService.getLogFiles(anyString(), anyString(), any(), anyString(), any(), anyInt()))
                .thenReturn(mockResponse);

        ResponseEntity<OperationHistoryResponse> response = skillLookupsController.fetchSkillsImportHistory("createdOn", "desc", null, "skill-management", null, 5);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}