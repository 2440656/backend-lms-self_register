package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.TenantSettingsDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.TenantSettingsRequest;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.UpdateTenantFeatureFlagsRequest;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantSettingsServiceImplTest {

  private TenantSettingsDao tenantSettingsDao;
  private TenantSettingsServiceImpl service;

  @BeforeEach
  void setUp() {
    tenantSettingsDao = mock(TenantSettingsDao.class);
    service = new TenantSettingsServiceImpl(tenantSettingsDao);
  }

  @Test
  void testGetTenantSettingResponseReturnsResult() {
    TenantSettingsResponse expected = new TenantSettingsResponse();
    expected.setReviewEmail("test@example.com");

    when(tenantSettingsDao.getTenant("config")).thenReturn(expected);

    TenantSettingsResponse actual = service.getTenantSettingResponse("config");

    assertNotNull(actual);
    assertEquals("test@example.com", actual.getReviewEmail());
  }

  @Test
  void testGetTenantSettingResponseReturnsNullIfNotFound() {
    when(tenantSettingsDao.getTenant("unknown")).thenReturn(null);
    assertNull(service.getTenantSettingResponse("unknown"));
  }

  @Test
  void testUpdateTenantSettingsWithInvalidEmail() {
    String badEmail = "bad-format";
    String result = service.updateTenantSettings(badEmail, "Manual");
    assertTrue(result.startsWith("Invalid Email format"));
  }

  @Test
  void testUpdateTenantSettingsDaoReturnsFalse() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class);
         MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {

      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
      mockedUserContext.when(UserContext::getCreatedBy).thenReturn("user");

      when(tenantSettingsDao.updateTenantSetings(any(Tenant.class), anyString(), anyString()))
          .thenReturn(false);

      String result = service.updateTenantSettings("valid@email.com", "TypeX");
      assertEquals("Failed to update tenant settings.", result);
    }
  }

  @Test
  void testCreateTenantSettingsSuccess() {
    try (MockedStatic<TenantUtil> mockTenantUtil = Mockito.mockStatic(TenantUtil.class);
         MockedStatic<UserContext> mockUserContext = Mockito.mockStatic(UserContext.class)) {

      TenantSettingsRequest request = new TenantSettingsRequest();
      request.setReviewEmail("user@example.com");
      request.setCourseReviewCommentType("Auto");

      mockTenantUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
      mockUserContext.when(UserContext::getCreatedBy).thenReturn("admin");

      when(tenantSettingsDao.createTenantSettings(any(Tenant.class)))
          .thenReturn(true);

      String result = service.createTenantSettings(request);
      assertEquals("Tenant Settings Created Successfully.", result);
    }
  }

  @Test
  void testCreateTenantSettingsWithInvalidEmail() {
    TenantSettingsRequest request = new TenantSettingsRequest();
    request.setReviewEmail("invalid-email");
    request.setCourseReviewCommentType("Manual");

    String result = service.createTenantSettings(request);
    assertTrue(result.startsWith("Invalid Email format"));
  }

  @Test
  void testCreateTenantSettingsReturnsFalseFromDao() {
    try (MockedStatic<TenantUtil> mockTenantUtil = Mockito.mockStatic(TenantUtil.class);
         MockedStatic<UserContext> mockUserContext = Mockito.mockStatic(UserContext.class)) {

      TenantSettingsRequest request = new TenantSettingsRequest();
      request.setReviewEmail("valid@example.com");
      request.setCourseReviewCommentType("Inline");

      mockTenantUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
      mockUserContext.when(UserContext::getCreatedBy).thenReturn("admin");

      when(tenantSettingsDao.createTenantSettings(any(Tenant.class)))
          .thenReturn(false);

      String result = service.createTenantSettings(request);
      assertEquals("Error creating tenant settings.", result);
    }
  }

  @Test
  void testCreateTenantSettingsThrowsRuntimeException() {
    try (MockedStatic<TenantUtil> mockTenantUtil = Mockito.mockStatic(TenantUtil.class);
         MockedStatic<UserContext> mockUserContext = Mockito.mockStatic(UserContext.class)) {

      TenantSettingsRequest request = new TenantSettingsRequest();
      request.setReviewEmail("valid@example.com");
      request.setCourseReviewCommentType("Auto");

      mockTenantUtil.when(TenantUtil::getTenantCode).thenReturn("demo");
      mockUserContext.when(UserContext::getCreatedBy).thenReturn("admin");

      when(tenantSettingsDao.createTenantSettings(any(Tenant.class)))
          .thenThrow(new RuntimeException("boom"));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> service.createTenantSettings(request));

      assertTrue(exception.getMessage().contains("Error creating tenant settings: boom"));
    }
  }

  // Tests for getTenantFeatureFlags
  @Test
  void testGetTenantFeatureFlagsSuccess() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      Map<String, Boolean> flags = new HashMap<>();
      flags.put("aiAssistant", true);
      flags.put("learningPaths", false);
      TenantFeatureFlagsDto dto = TenantFeatureFlagsDto.builder()
          .pk("demo")
          .sk("demo#featureFlags")
          .tenant("demo")
          .name("featureFlags")
          .featureFlags(flags)
          .build();
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(dto);
      TenantFeatureFlagsDto result = service.getTenantFeatureFlags();
      assertNotNull(result);
      assertEquals("demo", result.getPk());
      assertEquals("featureFlags", result.getName());
      assertEquals(Boolean.TRUE, result.getFeatureFlags().get("aiAssistant"));
      assertEquals(Boolean.FALSE, result.getFeatureFlags().get("learningPaths"));
    }
  }

  @Test
  void testGetTenantFeatureFlagsReturnsNull() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(null);
      TenantFeatureFlagsDto result = service.getTenantFeatureFlags();
      assertNull(result);
    }
  }

  @Test
  void testGetTenantFeatureFlagsThrowsRuntimeException() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenThrow(new RuntimeException("db failure"));
      RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getTenantFeatureFlags());
      assertTrue(ex.getMessage().contains("Error retrieving tenant feature flags: db failure"));
    }
  }

  // New tests for updateTenantFeatureFlags
  @Test
  void testUpdateTenantFeatureFlagsWithNullRequest() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      TenantFeatureFlagsDto current = TenantFeatureFlagsDto.builder().pk("demo").name("featureFlags").featureFlags(new HashMap<>()).build();
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(current);
      TenantFeatureFlagsDto result = service.updateTenantFeatureFlags(null);
      assertSame(current, result);
      verify(tenantSettingsDao, times(1)).getTenantFeatureFlags("cognizant");
      verify(tenantSettingsDao, never()).updateTenantFeatureFlags(any(), anyString());
    }
  }

  @Test
  void testUpdateTenantFeatureFlagsWithNullMap() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      TenantFeatureFlagsDto current = TenantFeatureFlagsDto.builder().pk("demo").name("featureFlags").featureFlags(new HashMap<>()).build();
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(current);
      UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(null);
      TenantFeatureFlagsDto result = service.updateTenantFeatureFlags(request);
      assertSame(current, result);
      verify(tenantSettingsDao, times(1)).getTenantFeatureFlags("cognizant");
      verify(tenantSettingsDao, never()).updateTenantFeatureFlags(any(), anyString());
    }
  }

  @Test
  void testUpdateTenantFeatureFlagsSuccess() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      Map<String, Boolean> updateMap = new HashMap<>();
      updateMap.put("aiAssistant", true);
      UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(updateMap);
      when(tenantSettingsDao.updateTenantFeatureFlags(updateMap, "cognizant")).thenReturn(true);
      Map<String, Boolean> finalMap = new HashMap<>();
      finalMap.put("aiAssistant", true);
      TenantFeatureFlagsDto finalDto = TenantFeatureFlagsDto.builder().pk("demo").name("featureFlags").featureFlags(finalMap).build();
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(finalDto);
      TenantFeatureFlagsDto result = service.updateTenantFeatureFlags(request);
      assertEquals(Boolean.TRUE, result.getFeatureFlags().get("aiAssistant"));
      verify(tenantSettingsDao, times(1)).updateTenantFeatureFlags(updateMap, "cognizant");
      verify(tenantSettingsDao, times(1)).getTenantFeatureFlags("cognizant");
    }
  }

  @Test
  void testUpdateTenantFeatureFlagsDaoReturnsFalse() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      Map<String, Boolean> updateMap = new HashMap<>();
      updateMap.put("learningPaths", false);
      UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(updateMap);
      when(tenantSettingsDao.updateTenantFeatureFlags(updateMap, "cognizant")).thenReturn(false);
      TenantFeatureFlagsDto afterDto = TenantFeatureFlagsDto.builder().pk("demo").name("featureFlags").featureFlags(updateMap).build();
      when(tenantSettingsDao.getTenantFeatureFlags("cognizant")).thenReturn(afterDto);
      TenantFeatureFlagsDto result = service.updateTenantFeatureFlags(request);
      assertEquals(Boolean.FALSE, result.getFeatureFlags().get("learningPaths"));
      verify(tenantSettingsDao, times(1)).updateTenantFeatureFlags(updateMap, "cognizant");
      verify(tenantSettingsDao, times(1)).getTenantFeatureFlags("cognizant");
    }
  }

  @Test
  void testUpdateTenantFeatureFlagsThrowsRuntimeException() {
    try (MockedStatic<TenantUtil> mockedTenantUtil = Mockito.mockStatic(TenantUtil.class)) {
      mockedTenantUtil.when(TenantUtil::getTenantCode).thenReturn("cognizant");
      Map<String, Boolean> updateMap = new HashMap<>();
      updateMap.put("aiAssistant", true);
      UpdateTenantFeatureFlagsRequest request = new UpdateTenantFeatureFlagsRequest(updateMap);
      when(tenantSettingsDao.updateTenantFeatureFlags(updateMap, "cognizant")).thenThrow(new RuntimeException("write failed"));
      RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateTenantFeatureFlags(request));
      assertTrue(ex.getMessage().contains("Error updating tenant feature flags: write failed"));
    }
  }
}
