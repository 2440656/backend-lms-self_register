package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cognizant.lms.userservice.dao.OperationsHistoryDao;
import com.cognizant.lms.userservice.dao.SkillLookupsDao;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.*;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cognizant.lms.userservice.utils.FileUtil;
import com.cognizant.lms.userservice.utils.S3Util;
import com.cognizant.lms.userservice.utils.SkillCSVProcessor;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

public class SkillLookupsServiceImplTest {

  @Mock
  private SkillLookupsDao skillLookupsDao;

  @Mock
  private OperationsHistoryDao operationsHistoryDao;

  @Mock
  private SkillCSVProcessor skillCSVProcessor;

  @Mock
  private S3Util s3Util;

  @Mock
  private FileUtil fileUtil;

  @Mock
  private Authentication authentication;

  @Mock
  private AuthUser authUser;

  @Mock
  private SecurityContext securityContext;

  @InjectMocks
  private SkillLookupsServiceImpl skillLookupsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("lms-admin");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    skillLookupsService = new SkillLookupsServiceImpl(
            skillLookupsDao,
            operationsHistoryDao,
            skillCSVProcessor,
            "test-bucket", // bucketName
            "/tmp",                   // localStoragePath
            "local",                  // applicationEnv (should match Constants.appEnv if you want local logic)
            s3Util,
            fileUtil
    );
  }

  @Test
  void getSkillCategory_withValidSkillName_returnsSkillCategory() {
    String skillName = "java";
    List<SkillCategoryResponse> mockCategory = List.of(new SkillCategoryResponse());
    when(skillLookupsDao.getSkillCategory(skillName)).thenReturn(mockCategory);
    List<SkillCategoryResponse> result = skillLookupsService.getSkillCategory(skillName);
    assertEquals(mockCategory, result);
  }

  @Test
  void getSkillCategory_withEmptySkillName_returnsEmptyList() {
    String skillName = "";
    List<SkillCategoryResponse> mockCategory = List.of();
    when(skillLookupsDao.getSkillCategory(skillName)).thenReturn(mockCategory);
    List<SkillCategoryResponse> result = skillLookupsService.getSkillCategory(skillName);
    assertEquals(mockCategory, result);
  }

  @Test
  void getSkillCategory_withNullSkillName_returnsEmptyList() {
    String skillName = null;
    List<SkillCategoryResponse> mockCategory = List.of();
    when(skillLookupsDao.getSkillCategory(skillName)).thenReturn(mockCategory);
    List<SkillCategoryResponse> result = skillLookupsService.getSkillCategory(skillName);
    assertEquals(mockCategory, result);
  }

  @Test
  void getSkillCategory_withNonExistentSkillName_returnsEmptyList() {
    String skillName = "nonExistentSkill";
    List<SkillCategoryResponse> mockCategory = List.of();
    when(skillLookupsDao.getSkillCategory(skillName)).thenReturn(mockCategory);
    List<SkillCategoryResponse> result = skillLookupsService.getSkillCategory(skillName);
    assertEquals(mockCategory, result);
  }

  @Test
  void getSkillsLookupsByTypeNameOrCode_normalizesSearch() {
    String type = "Skill";
    String search = "  Java  ";
    SkillLookupResponse mockResponse = new SkillLookupResponse(List.of(), null, 200, null);
    when(skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, "java")).thenReturn(mockResponse);

    SkillLookupResponse result = skillLookupsService.getSkillsLookupsByTypeNameOrCode(type, search);

    assertEquals(mockResponse, result);
    verify(skillLookupsDao).getSkillsAndLookupsByNameOrCode(type, "java");
  }

  @Test
  void getSkillsLookupsByTypeNameOrCode_withNullSearch_passesNull() {
    String type = "Skill-Cat";
    SkillLookupResponse mockResponse = new SkillLookupResponse(List.of(), null, 200, null);
    when(skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, null)).thenReturn(mockResponse);

    SkillLookupResponse result = skillLookupsService.getSkillsLookupsByTypeNameOrCode(type, null);

    assertEquals(mockResponse, result);
    verify(skillLookupsDao).getSkillsAndLookupsByNameOrCode(type, null);
  }

  @Test
  void uploadSkills_success() throws Exception {
      MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn("skills.csv");
      SkillsCSVProcessResponse mockProcessResponse = mock(SkillsCSVProcessResponse.class);
      when(mockProcessResponse.getSuccessCount()).thenReturn(1);
      when(mockProcessResponse.getFailureCount()).thenReturn(0);
      when(mockProcessResponse.getTotalCount()).thenReturn(1);
      when(skillCSVProcessor.processFile(mockFile)).thenReturn(mockProcessResponse);
      when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);

      FileUploadResponse response = skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS");
      assertNotNull(response);
      assertNull(response.getErrorLogFileName());
  }

  @Test
  void uploadSkills_partialFailure() throws Exception {
      MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn("skills.csv");
      SkillsCSVProcessResponse mockProcessResponse = mock(SkillsCSVProcessResponse.class);
      when(mockProcessResponse.getSuccessCount()).thenReturn(1);
      when(mockProcessResponse.getFailureCount()).thenReturn(1);
      when(mockProcessResponse.getTotalCount()).thenReturn(2);
      when(skillCSVProcessor.processFile(mockFile)).thenReturn(mockProcessResponse);
      when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);

      assertThrows(Exception.class, () -> skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS"));
  }

  @Test
  void uploadSkills_failure() throws Exception {
      MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn("skills.csv");
      SkillsCSVProcessResponse mockProcessResponse = mock(SkillsCSVProcessResponse.class);
      when(mockProcessResponse.getSuccessCount()).thenReturn(0);
      when(mockProcessResponse.getFailureCount()).thenReturn(1);
      when(mockProcessResponse.getTotalCount()).thenReturn(1);
      when(skillCSVProcessor.processFile(mockFile)).thenReturn(mockProcessResponse);
      when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);

      assertThrows(Exception.class, () -> skillLookupsService.uploadSkills(mockFile, "UPLOAD_SKILLS"));
  }

  @Test
  void getDownloadErrorLogFileForSkills_localTxt_fileNotFound() {
      String filename = "missing.txt";
      String fileType = "txt";
      assertThrows(FileNotFoundException.class, () -> skillLookupsService.getDownloadErrorLogFileForSkills(filename, fileType));
  }

  @Test
  void getDownloadErrorLogFileForSkills_unsupportedType() {
      String filename = "file.unknown";
      String fileType = "unknown";
      assertThrows(IllegalArgumentException.class, () -> skillLookupsService.getDownloadErrorLogFileForSkills(filename, fileType));
  }

}