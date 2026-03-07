package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cognizant.lms.userservice.client.CourseManagementServiceClient;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dao.OperationsHistoryDao;
import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.TeanatTableDao;
import com.cognizant.lms.userservice.dao.UserActivityLogDao;
import com.cognizant.lms.userservice.dao.UserDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.EmailOtpActionResponse;
import com.cognizant.lms.userservice.dto.EmailOtpResendRequest;
import com.cognizant.lms.userservice.dto.EmailOtpVerificationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationResponse;
import com.cognizant.lms.userservice.dto.TenantConfigDto;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.exception.ValidationException;
import com.cognizant.lms.userservice.utils.CSVProcessor;
import com.cognizant.lms.userservice.utils.CSVValidator;
import com.cognizant.lms.userservice.utils.FileUtil;
import com.cognizant.lms.userservice.utils.S3Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

class UserSelfRegistrationServiceTest {

  private UserServiceImpl userService;
  private UserFilterSortDao userFilterSortDao;
  private UserDao userDao;
  private TeanatTableDao teanatTableDao;
  private CognitoAsyncService cognitoAsyncService;

  @BeforeEach
  void setUp() {
    userDao = mock(UserDao.class);
    userFilterSortDao = mock(UserFilterSortDao.class);
    cognitoAsyncService = mock(CognitoAsyncService.class);
    teanatTableDao = mock(TeanatTableDao.class);

    userService = new UserServiceImpl(
        userDao,
        userFilterSortDao,
        cognitoAsyncService,
        mock(S3Util.class),
        mock(CSVProcessor.class),
        mock(FileUtil.class),
        mock(RoleDao.class),
        mock(LookupDao.class),
        Constants.appEnv,
        "bucket",
        "local",
        mock(OperationsHistoryDao.class),
        mock(CourseManagementServiceClient.class),
        mock(UserManagementEventPublisherService.class),
        mock(CSVValidator.class),
        mock(UserActivityLogService.class),
        mock(UserActivityLogDao.class),
        teanatTableDao,
        "rootDomainPath");

    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenant.acme.com", "Acme",
        "skillspring credentials,social idp", "portal", "client", "issuer", "cert"));
  }

  @Test
  void registerUserByEmail_shouldCreateUserInLocalEnvironment() {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");

    TenantConfigDto tenantConfigDto = new TenantConfigDto();
    tenantConfigDto.setUserPoolAppId("app-client-id");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfigDto);
    when(userFilterSortDao.getUserByEmailIdAndTenant("john.doe@example.com", Constants.ACTIVE_STATUS, "t-2"))
        .thenReturn(null);
    when(userFilterSortDao.getUserByEmailIdAndTenant("john.doe@example.com", Constants.IN_ACTIVE_STATUS, "t-2"))
        .thenReturn(null);
    when(cognitoAsyncService.signUpUserAsync(anyString(), anyString(), anyString(), anyString(),
        anyString(), nullable(String.class))).thenReturn(CompletableFuture.completedFuture(null));

    EmailSelfRegistrationResponse response = userService.registerUserByEmail(request);

    assertNotNull(response.getUserId());
    assertTrue(response.isEmailVerificationRequired());
    verify(userDao).createUser(org.mockito.ArgumentMatchers.any(User.class));
    verify(cognitoAsyncService).signUpUserAsync("john.doe@example.com", "Strong@123", "John",
        "Doe", "app-client-id", null);
  }

  @Test
  void registerUserByEmail_shouldThrowWhenEmailAlreadyExists() {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");

    TenantConfigDto tenantConfigDto = new TenantConfigDto();
    tenantConfigDto.setUserPoolAppId("app-client-id");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfigDto);
    when(userFilterSortDao.getUserByEmailIdAndTenant("john.doe@example.com", Constants.ACTIVE_STATUS, "t-2"))
        .thenReturn(new User());

    assertThrows(ValidationException.class, () -> userService.registerUserByEmail(request));
  }

  @Test
  void registerUserByEmail_shouldThrowWhenPasswordIsWeak() {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "weak",
        "weak");

    assertThrows(ValidationException.class, () -> userService.registerUserByEmail(request));
  }

  @Test
  void registerUserByEmail_shouldAllowWhenTenantIdpPreferencesMissing() {
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenant.acme.com", "Acme",
        null, "portal", "client", "issuer", "cert"));

    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "Jane", "Doe", "jane.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");

    TenantConfigDto tenantConfigDto = new TenantConfigDto();
    tenantConfigDto.setUserPoolAppId("app-client-id");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfigDto);
    when(userFilterSortDao.getUserByEmailIdAndTenant("jane.doe@example.com", Constants.ACTIVE_STATUS, "t-2"))
        .thenReturn(null);
    when(userFilterSortDao.getUserByEmailIdAndTenant("jane.doe@example.com", Constants.IN_ACTIVE_STATUS, "t-2"))
        .thenReturn(null);
    when(cognitoAsyncService.signUpUserAsync(anyString(), anyString(), anyString(), anyString(),
        anyString(), nullable(String.class))).thenReturn(CompletableFuture.completedFuture(null));

    EmailSelfRegistrationResponse response = userService.registerUserByEmail(request);

    assertNotNull(response.getUserId());
    assertTrue(response.isEmailVerificationRequired());
    verify(userDao).createUser(org.mockito.ArgumentMatchers.any(User.class));
  }

  @Test
  void verifyRegistrationOtp_shouldVerifyOtpInCognito() {
    TenantConfigDto tenantConfigDto = new TenantConfigDto();
    tenantConfigDto.setUserPoolAppId("app-client-id");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfigDto);
    when(cognitoAsyncService.confirmSignUpAsync(anyString(), anyString(), anyString(),
        nullable(String.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    EmailOtpActionResponse response = userService.verifyRegistrationOtp(
        new EmailOtpVerificationRequest("john.doe@example.com", "123456"));

    assertTrue(response.isSuccess());
    verify(cognitoAsyncService).confirmSignUpAsync("john.doe@example.com", "123456",
        "app-client-id", null);
  }

  @Test
  void resendRegistrationOtp_shouldResendOtpInCognito() {
    TenantConfigDto tenantConfigDto = new TenantConfigDto();
    tenantConfigDto.setUserPoolAppId("app-client-id");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfigDto);
    when(cognitoAsyncService.resendSignUpOtpAsync(anyString(), anyString(), nullable(String.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    EmailOtpActionResponse response = userService.resendRegistrationOtp(
        new EmailOtpResendRequest("john.doe@example.com"));

    assertTrue(response.isSuccess());
    verify(cognitoAsyncService).resendSignUpOtpAsync("john.doe@example.com", "app-client-id",
        null);
  }

  @Test
  void registerUserByEmail_shouldThrowWhenTenantConfigClientIdMissing() {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");

    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(new TenantConfigDto());
    when(userFilterSortDao.getUserByEmailIdAndTenant("john.doe@example.com", Constants.ACTIVE_STATUS, "t-2"))
        .thenReturn(null);
    when(userFilterSortDao.getUserByEmailIdAndTenant("john.doe@example.com", Constants.IN_ACTIVE_STATUS, "t-2"))
        .thenReturn(null);

    assertThrows(ValidationException.class, () -> userService.registerUserByEmail(request));
    verify(cognitoAsyncService, never()).signUpUserAsync(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyString());
  }
}
