package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cognizant.lms.userservice.client.CourseManagementServiceClient;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.utils.UserContext;
import org.mockito.MockedStatic;
import com.cognizant.lms.userservice.dao.*;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.FileProcessingException;
import com.cognizant.lms.userservice.exception.FileStorageException;
import com.cognizant.lms.userservice.exception.UserNotFoundException;
import com.cognizant.lms.userservice.exception.ValidationException;
import com.cognizant.lms.userservice.utils.CSVProcessor;
import com.cognizant.lms.userservice.utils.CSVValidator;
import com.cognizant.lms.userservice.utils.FileUtil;
import com.cognizant.lms.userservice.utils.S3Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.cognizant.lms.userservice.utils.TenantUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(SpringExtension.class)
public class UserServiceImplTest {

  private static final Logger log = LoggerFactory.getLogger(UserServiceImplTest.class);
  private UserDao userDao;
  private HttpServletRequest httpServletRequest;
  private HttpServletResponse httpServletResponse;

  private S3Util s3Utils;

  private CognitoAsyncService cognitoService;

  private UserFilterSortDao userFilterSortDao;

  private UserActivityLogService userActivityLogService;

  private UserActivityLogDao userActivityLogDao;

  private TeanatTableDao teanatTableDao;

  private CSVProcessor csvProcessor;

  private CSVValidator csvValidator;

  private UserServiceImpl userServiceImpl;

  private UserManagementEventPublisherService userManagementEventPublisherService;

  private FileUtil fileUtil;

  private RoleDao roleDao;

  private LookupDao lookupDao;

  private OperationsHistoryDao logFileDao;

  private String applicationEnv;
  private String bucketName;
  private String storageBucket;
  private Authentication authentication;
  private AuthUser authUser;
  private SecurityContext securityContext;
  @Mock
  private CourseManagementServiceClient courseManagementServiceClient;

  @BeforeEach
  void init() {
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
    csvValidator = mock(CSVValidator.class);
    csvProcessor = mock(CSVProcessor.class);
    s3Utils = mock(S3Util.class);
    fileUtil = mock(FileUtil.class);
    userDao = mock(UserDao.class);
    cognitoService = mock(CognitoAsyncService.class);
    userFilterSortDao = mock(UserFilterSortDao.class);
    userActivityLogService = mock(UserActivityLogService.class);
    userActivityLogDao = mock(UserActivityLogDao.class);
    teanatTableDao = mock(TeanatTableDao.class);
    roleDao = mock(RoleDao.class);
    lookupDao = mock(LookupDao.class);
    applicationEnv = "local";
    bucketName = "lms-user-service";
    storageBucket = "lms-user-service";
    logFileDao = mock(OperationsHistoryDao.class);
    securityContext = mock(SecurityContext.class);
    authentication = mock(Authentication.class);
    authUser = mock(AuthUser.class);
    httpServletResponse = mock(HttpServletResponse.class);
    httpServletRequest = mock(HttpServletRequest.class);
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(authUser);
    when(authUser.getUsername()).thenReturn("lms-admin");
    when(authUser.getUserEmail()).thenReturn("testEmail");
    userServiceImpl =
        new UserServiceImpl(userDao, userFilterSortDao, cognitoService, s3Utils, csvProcessor,
            fileUtil, roleDao, lookupDao, applicationEnv, bucketName, storageBucket, logFileDao, courseManagementServiceClient,
            userManagementEventPublisherService,csvValidator, userActivityLogService, userActivityLogDao, teanatTableDao, "rootDomainPath");
  }

  @Test
  @DisplayName("Test uploadUsers method with uploadBulkUsersFile failed to save")
  public void testUploadUsers_uploadBulkUsersFileFailed() {
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(file, storageBucket, "testPath")).thenReturn(false);
    Exception exception = assertThrows(FileStorageException.class, () -> {
      userServiceImpl.uploadUsers(file, "Add Users");
    });
    assertEquals("Failed to save file" + file.getOriginalFilename(), exception.getMessage());
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local"
      + " and processed successfully")
  public void testUploadUsers_success_local() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("India") // Added country field
        .build();

    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    FileUploadResponse fileUploadResponse =
        userServiceImpl.uploadUsers(file, "Add Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 1 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userDao, times(1)).createUser(user);
  }

  @Test
  @DisplayName(
      "Test uploadUsers method with file saved successfully in local and successfully"
          + " validated CSV file but user already exists in DB")
  public void testUploadUsers_userExistInDB()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(List.of());
    expectedResponse.setErrors(List.of("User already exists -- abc.axy@cognizant.com"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    when(userDao.userExists("abc.xyz@cognizant.com")).thenReturn(true);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileProcessingException e) {
      assertEquals("File test.csv failed with 1 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class),
        anyString(), anyString());
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local and successfully"
      + " validated CSV file but user creation failed")
  public void testUploadUsers_createUserFailedInDB()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user1 =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "mentor",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("mentor")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("England") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    doThrow(new RuntimeException("Error")).when(userDao).createUser(user);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (RuntimeException e) {
      assertEquals("Unable to add User " + user.getPk() + " with error : Error",
          e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userDao, times(1)).createUser(user);
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in s3 and processed"
      + " successfully in dev environment")
  public void testUploadUsers_success_dev()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.createCognitoUserAsync(user)).thenReturn(future);
    FileUploadResponse fileUploadResponse =
        userServiceImpl.uploadUsers(file, "Add Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 1 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(userDao, times(1)).createUser(user);
    verify(cognitoService, times(1)).createCognitoUserAsync(user);
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in s3 and successfully"
      + " validated CSV file but cognito user creation failed")
  public void testUploadUsers_createCognitoUsersFailed()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12-12-2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Australia") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new ExecutionException(new Throwable("Error")));
    when(cognitoService.createCognitoUserAsync(user)).thenReturn(future);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (RuntimeException e) {
      assertEquals("Failed to create user in cognito abc.xyz@cognizant.com",
          e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(cognitoService, times(1)).createCognitoUserAsync(user);
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local but CSV file"
      + " validation failed and error logs creation is successful in local")
  public void testUploadUsers_handleErrLoggingSuccess_local()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(0);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--Invalid Header"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename() + " failed with 1 errors.",
          e.getMessage());
    }
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class),
        anyString(), anyString());
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local but CSV file"
      + " validation failed and error logs creation failed in local")
  public void testUploadUsers_handleErrLoggingFailed_local()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(0);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--Invalid Header"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now()
        .format(DateTimeFormatter
            .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileStorageException e) {
      assertEquals("Failed to save file : " + errorLogFileName, e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in s3 and CSV file"
      + " validation failed but error logs creation is successful in s3")
  public void testUploadUsers_handleErrLoggingSuccess_dev()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(0);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--Invalid Header"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename() + " failed with 1 errors.",
          e.getMessage());
    }
    verify(s3Utils, times(2)).saveFileToS3(any(MultipartFile.class),
        anyString(), anyString());
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local but CSV file"
      + " validation is partial with errors and validUsers")
  public void testUploadUsers_handleUploadingUsersAndLoggingSuccess() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", null,
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role(null)
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("India") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--alex@gmail.com--alex12--brown--FirstName must contain"
            + " only alphabetic characters at row 2"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename() + " processed partially with "
          + "1 records verified and 1 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userDao, times(1)).createUser(user);
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local but CSV file"
      + " validation is partial with errors and validUsers and error logs creation failed in local")
  public void testUploadUsers_uploadingUserSuccess_loggingFailed()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("India") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--alex@gmail.com--alex12--brown--FirstName must contain"
            + " only alphabetic characters at row 2"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now()
        .format(DateTimeFormatter
            .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileStorageException e) {
      assertEquals("Failed to save file : " + errorLogFileName, e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(userDao, times(1)).createUser(user);
  }

  @Test
  @DisplayName("Test uploadUsers method with file saved successfully in local but CSV file"
      + " validation is partial with errors and validUsers and user already exists in DB")
  public void testUploadUsers_uploadingUserFailed()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(List.of());
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--alex@gmail.com--alex12--brown--FirstName must contain"
            + " only alphabetic characters at row 2"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "EmailID\nalex@gmail.com".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processAddUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId(anyString(), anyString())).thenReturn(new User());

    try {
      userServiceImpl.uploadUsers(file, "Add Users");
    } catch (FileProcessingException e) {
      assertEquals("File test.csv failed with 1 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processAddUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class),
        anyString(), anyString());
  }

  @Test
  public void testDeactivateUsers_fileSavedFailed() {
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(file, storageBucket, "testPath")).thenReturn(false);
    Exception exception = assertThrows(FileStorageException.class, () -> {
      userServiceImpl.deactivateUser(file, "De-Activate Users");
    });
    assertEquals("Failed to save file" + file.getOriginalFilename(), exception.getMessage());
  }

  @Test
  public void testDeactivateUsers_fileProcessSuccess() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(2);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gamil.com"));
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    User user1 =
        new User(
            "abc", "xyz",
            "Cognizant", "abc.xyz@cognizant.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    User user2 =
        new User(
            "qwerty", "xyz",
            "Cognizant", "qwerty@gamil.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gamil.com", "Active")).thenReturn(user2);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(true);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(true);
    FileUploadResponse fileUploadResponse =
        userServiceImpl.deactivateUser(file, "De-Activate Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 2 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gamil.com", "Active");
    verify(userFilterSortDao, times(1)).deactivateUser(user1, "lms-admin");
    verify(userFilterSortDao, times(1)).deactivateUser(user2, "lms-admin");
  }

  @Test
  public void testDeactivateUsers_UserNotFoundInDB()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(2);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gamil.com"));
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(null);
    try {
      userServiceImpl.deactivateUser(file, "De-Activate Users");
    } catch (FileProcessingException e) {
      assertEquals("No valid user email found to deactivate in the database."
          + " Please check the csv file and try again.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
  }

  @Test
  public void testDeactivateUsers_deactivateUserFailedFromDB()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(2);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gamil.com"));
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    User user1 =
        new User(
            "abc", "xyz",
            "Cognizant", "abc.xyz@cognizant.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    User user2 =
        new User(
            "qwerty", "xyz",
            "Cognizant", "qwerty@gamil.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gamil.com", "Active")).thenReturn(user2);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(false);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(false);
    try {
      userServiceImpl.deactivateUser(file, "De-Activate Users");
    } catch (FileProcessingException e) {
      assertEquals("No valid user email found to deactivate in the database."
          + " Please check the csv file and try again.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gamil.com", "Active");
    verify(userFilterSortDao, times(1)).deactivateUser(user1, "lms-admin");
    verify(userFilterSortDao, times(1)).deactivateUser(user2, "lms-admin");
  }

  @Test
  public void testDeactivateUsers_fileProcessedSuccessS3() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(2);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gamil.com"));
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    User user1 =
        new User(
            "abc", "xyz",
            "Cognizant", "abc.xyz@cognizant.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    User user2 =
        new User(
            "qwerty", "xyz",
            "Cognizant", "qwerty@gamil.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gamil.com", "Active")).thenReturn(user2);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.disableCognitoUserAsync(user1)).thenReturn(future);
    when(cognitoService.disableCognitoUserAsync(user2)).thenReturn(future);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(true);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(true);
    FileUploadResponse fileUploadResponse =
        userServiceImpl.deactivateUser(file, "De-Activate Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 2 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gamil.com", "Active");
    verify(cognitoService, times(1)).disableCognitoUserAsync(user1);
    verify(cognitoService, times(1)).disableCognitoUserAsync(user2);
    verify(userFilterSortDao, times(1)).deactivateUser(user1, "lms-admin");
    verify(userFilterSortDao, times(1)).deactivateUser(user2, "lms-admin");
  }


  @Test
  public void testDeactivateUsers_deleteCognitoFailed() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");

    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(true);

    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com"));

    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));

    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);

    User user = new User(
        "abc", "xyz", "Cognizant", "abc.xyz@cognizant.com",
        "Internal", "learner, mentor", "12-12-2025", "Y", "Skillspring Credentials");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);

    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new ExecutionException(new Throwable("Error")));
    when(cognitoService.disableCognitoUserAsync(user)).thenReturn(failedFuture);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.deactivateUser(file, "De-Activate Users");
    });

    assertEquals("No valid user email found to deactivate in the database. Please check the csv file and try again.", thrown.getMessage());

    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(cognitoService, times(1)).disableCognitoUserAsync(user);
  }


  @Test
  public void testDeactivateUsers_errorLogLocalFailed() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com"));
    expectedResponse.setErrors(
        List.of("09-28-24 11:51:15--alex@gmail.com--Error in processing at row 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(null);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals(
          "No valid user data email to deactivate in the database."
              + " Please check the csv file and try again.",
          e.getMessage());
    }

    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
  }

  @Test
  public void testDeactivateUsers_fileValidationFailed() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(2);
    expectedResponse.setValidEmail(Set.of());
    expectedResponse.setErrors(
        List.of("09-28-24 11:51:15--alex@gmail.com--Error in processing at row 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals("File test.csv failed with 2 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(0)).getUserByEmailId(anyString(), anyString());
  }

  @Test
  public void testDeactivateUsers_noValidUsersForDeactivation() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(0);
    expectedResponse.setValidEmail(Set.of());
    expectedResponse.setErrors(List.of());
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
      fail("Expected FileProcessingException");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename() + " failed with "
          + expectedResponse.getFailureCount() + " errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(0)).getUserByEmailId(anyString(),
        anyString()); // No user lookups needed
  }

  @Test
  public void testDeactivateUsers_errorLogSaveFailureLocal() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        false); // Simulate saving failure
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(2);
    expectedResponse.setValidEmail(Set.of());
    expectedResponse.setErrors(List.of("error message 1", "error message 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
      fail("Expected FileStorageException");
    } catch (FileStorageException e) {
      assertTrue(e.getMessage().contains("Failed to save file"));
    }
  }

  @Test
  public void testDeactivateUsers_partialProcessingErrLog() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true); // Simulate successful saving
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("valid@email.com"));
    expectedResponse.setErrors(List.of("error message for row 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals(
          "No valid user data email to deactivate in the database."
              + " Please check the csv file and try again.",
          e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId(
        "valid@email.com", "Active"); // User lookup for successful email
  }

  @Test
  public void testDeactivateUsers_validEmailsPresent() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gmail.com"));
    expectedResponse.setErrors(
        List.of("09-28-24 11:51:15--alex@gmail.com--Error in processing at row 2"));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    User user1 = new User();
    user1.setEmailId("abc.xyz@cognizant.com");
    User user2 = new User();
    user2.setEmailId("qwerty@gmail.com");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gmail.com", "Active")).thenReturn(user2);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(true);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(true);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        +
        LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename()
          + " processed partially with 1 records verified and 1 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gmail.com", "Active");
    verify(userFilterSortDao, times(1)).deactivateUser(user1, "lms-admin");
    verify(userFilterSortDao, times(1)).deactivateUser(user2, "lms-admin");
  }

  @Test
  public void testDeactivateUser_errorLogLocalFailed()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(0);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--Invalid Header"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now()
        .format(DateTimeFormatter
            .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileStorageException e) {
      assertEquals("Failed to save file : " + errorLogFileName, e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
  }

  @Test
  public void testDeactivateUsers_EmailsPresent() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("abc.xyz@cognizant.com", "qwerty@gmail.com"));
    expectedResponse.setErrors(
        List.of("09-28-24 11:51:15--alex@gmail.com--Error in processing at row 2"));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    User user1 = new User();
    user1.setEmailId("abc.xyz@cognizant.com");
    User user2 = new User();
    user2.setEmailId("qwerty@gmail.com");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gmail.com", "Active")).thenReturn(user2);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(true);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(true);
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename()
          + " processed partially with 1 records verified and 1 errors.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gmail.com", "Active");
    verify(userFilterSortDao, times(1)).deactivateUser(user1, "lms-admin");
    verify(userFilterSortDao, times(1)).deactivateUser(user2, "lms-admin");
  }

  @Test
  public void testDeactivateUsers_S3SuccessErrorLogSave() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("valid@email.com"));
    expectedResponse.setErrors(List.of("timestamp -- error message for row 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);

    // Create a User object with loginOption set
    User user = new User();
    user.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS); // Set loginOption to avoid null
    when(userFilterSortDao.getUserByEmailId(anyString(), anyString())).thenReturn(user);

    when(cognitoService.disableCognitoUserAsync(any(User.class))).thenReturn(
        CompletableFuture.completedFuture(null));
    when(userFilterSortDao.deactivateUser(any(User.class), anyString())).thenReturn(true);
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(true);

    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;

    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileProcessingException e) {
      assertEquals("File " + file.getOriginalFilename()
          + " processed partially with 1 records verified and 1 errors.", e.getMessage());
    }
  }

  @Test
  public void testDeactivateUsers_partialProcessing() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidEmail(Set.of("valid@email.com"));
    expectedResponse.setErrors(List.of("timestamp -- error message for row 2"));
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
        "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processDeActiveUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId(anyString(), anyString())).thenReturn(new User());
    when(cognitoService.deleteCognitoUserAsync(any(User.class))).thenReturn(
        CompletableFuture.completedFuture(null));
    when(userFilterSortDao.deactivateUser(any(User.class), anyString())).thenReturn(true);
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenAnswer(
        new Answer<Boolean>() {
          private int count = 0;

          @Override
          public Boolean answer(InvocationOnMock invocation) {
            return count++ == 0;
          }
        });
    String expected = "Failed to save file : " + file.getOriginalFilename() + "_LOG_";
    try {
      userServiceImpl.deactivateUser(file, "Deactivate Users");
    } catch (FileStorageException e) {
      assertTrue(e.getMessage().contains(expected));
    }
    verify(csvProcessor, times(1)).processDeActiveUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId(
        "valid@email.com", "Active"); // User lookup for successful email
  }

  @Test
  @DisplayName("Test updateUsers method with uploadBulkUsersFile failed to save")
  public void testUpdateUsers_uploadBulkUsersFileFailed() {
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(file, storageBucket, "testPath")).thenReturn(false);
    Exception exception = assertThrows(FileStorageException.class, () -> {
      userServiceImpl.updateUsers(file, "Update Users");
    });
    assertEquals("Failed to save file" + file.getOriginalFilename(), exception.getMessage());
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and processed successfully")
  public void testUpdateUsers_success_local() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("India") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(true);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    FileUploadResponse fileUploadResponse =
        userServiceImpl.updateUsers(file, "update Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 1 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and processed successfully with email and firstName only")
  public void testUpdateUsers_success_local_with_email_firstName() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "",
//            "", "abc.xyz@cognizant.com",
//            "", "",
//            "", "", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("")
        .institutionName("")
        .emailId("abc.xyz@cognizant.com")
        .userType("")
        .role("")
        .userAccountExpiryDate("")
        .viewOnlyAssignedCourses("")
        .loginOption("Skillspring Credentials")
        .country("India") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(true);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    FileUploadResponse fileUploadResponse =
        userServiceImpl.updateUsers(file, "update Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 1 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and partially successful")
  public void testUpdateUsers_partial_success_local() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("India") //Updated country field
        .build();
//    User user1 = new User(
//        "abc123", "xyz",
//        "Cognizant", "user@gmail.com", "Internal", "super-admin", "12/12/2025","Y", "Skillspring Credentials");
//
    User user1 = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("user@gmail.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user, user1));
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--user@gmail.com--alex12--abc123--FirstName must contain"
            + " only alphabetic characters at row 2"));
    expectedResponse.setValidEmail(null);
    expectedResponse.setFailureCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setTotalCount(2);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(true);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    Exception exception = assertThrows(FileProcessingException.class, () -> {
      userServiceImpl.updateUsers(file, "update Users");
    });
    String expected = "File test.csv processed partially with 1 records verified and 1 errors.";
    assertEquals(expected, exception.getMessage());
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(2)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and processed successfully")
  public void testUpdateUsers_success_dev() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(true);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    FileUploadResponse fileUploadResponse =
        userServiceImpl.updateUsers(file, "update Users");
    assertEquals("File " + file.getOriginalFilename()
            + " processed successfully with 1 records verified and 0 errors.",
        fileUploadResponse.getSuccessMessage());
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and processed successfully but failed to update in DB")
  public void testUpdateUsers_success_local_but_failed_inDB() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(false);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    Exception exception = assertThrows(FileProcessingException.class, () -> {
      userServiceImpl.updateUsers(file, "update Users");
    });
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and partially successful but failed to update in DB")
  public void testUpdateUsers_partial_success_local_but_failed_inDB() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString()))
        .thenReturn(true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(false);
    when(cognitoService.updateCognitoUserAsync(user))
        .thenReturn(CompletableFuture.completedFuture(null));
    Exception exception = assertThrows(FileProcessingException.class, () -> {
      userServiceImpl.updateUsers(file, "update Users");
    });
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local"
      + " and processed successfully but failed to update in Cognito")
  public void testUpdateUsers_success_dev_but_cognito_failed() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(s3Utils.saveFileToS3(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(false);
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new ExecutionException(new Throwable("Error")));
    when(cognitoService.updateCognitoUserAsync(user)).thenReturn(future);
    Exception exception = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.updateUsers(file, "update Users");
    });
    String expected = "Failed to update cognito user with email " + user.getEmailId();
    assertEquals(expected, exception.getMessage());
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(s3Utils, times(1)).saveFileToS3(any(MultipartFile.class), anyString(), anyString());
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(cognitoService, times(1)).updateCognitoUserAsync(user);
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local but CSV file"
      + " validation failed and error logs creation failed in local")
  public void testUpdate_Users_handleErrLoggingFailed_local()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(0);
    expectedResponse.setSuccessCount(0);
    expectedResponse.setFailureCount(1);
    expectedResponse.setValidUsers(null);
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--Invalid Header"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now()
        .format(DateTimeFormatter
            .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.updateUsers(file, "Update Users");
    } catch (FileStorageException e) {
      assertEquals("Failed to save file : " + errorLogFileName, e.getMessage());
    }
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
  }

  @Test
  @DisplayName("Test updateUsers method with file saved successfully in local but CSV file"
      + " validation is partial with errors and validUsers and error logs creation failed in local")
  public void testUpdateUsers_uploadingUserSuccess_loggingFailed()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true).thenReturn(false);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(2);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(1);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(List.of(
        "09-28-24 11:51:15--alex@gmail.com--alex12--brown--FirstName must contain"
            + " only alphabetic characters at row 2"));
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user);
    when(userFilterSortDao.updateUser(user, "lms-admin")).thenReturn(true);
    String errorLogFileName = file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now()
        .format(DateTimeFormatter
            .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
    try {
      userServiceImpl.updateUsers(file, "Update Users");
    } catch (FileStorageException e) {
      assertEquals("Failed to save file : " + errorLogFileName, e.getMessage());
    }
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).updateUser(user, "lms-admin");
  }

  @Test
  @DisplayName(
      "Test updateUsers method with file saved successfully in local and successfully"
          + " validated CSV file but user not exists in DB")
  public void testUpdateUsers_userNotExistInDB()
      throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    when(fileUtil.saveFileToLocal(any(MultipartFile.class), anyString(), anyString())).thenReturn(
        true);
    CSVProcessResponse expectedResponse = new CSVProcessResponse();
    expectedResponse.setTotalCount(1);
    expectedResponse.setSuccessCount(1);
    expectedResponse.setFailureCount(0);
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", null,
//            "12-12-2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role(null)
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    expectedResponse.setValidUsers(List.of(user));
    expectedResponse.setErrors(null);
    expectedResponse.setValidEmail(null);
    MultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv",
            "test data".getBytes(StandardCharsets.UTF_8));
    when(csvProcessor.processUpdateUserFile(file)).thenReturn(expectedResponse);
    try {
      userServiceImpl.updateUsers(file, "Update Users");
    } catch (FileProcessingException e) {
      assertEquals("No valid user data found to update to the database."
          + " Please check the csv file and try again.", e.getMessage());
    }
    verify(csvProcessor, times(1)).processUpdateUserFile(eq(file));
    verify(fileUtil, times(1)).saveFileToLocal(any(MultipartFile.class),
        anyString(), anyString());
  }

  @Test
  void getUserByPk_ShouldReturnUser_WhenUserExists() {
    String partitionKeyValue = "123";
//    User user =
//        new User(
//            "abc", "xyz",
//            "Cognizant", "abc.xyz@cognizant.com",
//            "Internal", "super-admin",
//            "12/12/2025","Y", "Skillspring Credentials");
    User user = User.builder()
        .firstName("abc")
        .lastName("xyz")
        .institutionName("Cognizant")
        .emailId("abc.xyz@cognizant.com")
        .userType("Internal")
        .role("super-admin")
        .userAccountExpiryDate("12/12/2025")
        .viewOnlyAssignedCourses("Y")
        .loginOption("Skillspring Credentials")
        .country("Germany") //Updated country field
        .build();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(user);
    User userResponse = userServiceImpl.getUserByPk(partitionKeyValue);
    assertNotNull(userResponse);
    assertEquals("abc", userResponse.getFirstName());
    assertEquals("xyz", userResponse.getLastName());
    assertEquals("abc.xyz@cognizant.com", userResponse.getEmailId());
    assertEquals("Cognizant", userResponse.getInstitutionName());
    assertEquals("Internal", userResponse.getUserType());
    assertEquals("super-admin", userResponse.getRole());
    assertEquals("12/12/2025", userResponse.getUserAccountExpiryDate());
    assertEquals("Germany", userResponse.getCountry());
    verify(userFilterSortDao).getUserByPk(partitionKeyValue);
  }

  @Test
  void getUserByPk_ShouldReturnNull_WhenUserDoesNotExist() {
    String partitionKeyValue = "123";
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(null);
    User userResponse = userServiceImpl.getUserByPk(partitionKeyValue);
    assertNull(userResponse);
    verify(userFilterSortDao).getUserByPk(partitionKeyValue);
  }


  @Test
  void updateUserByPk_UserNotFound_ThrowsUserNotFoundException() {
    String partitionKeyValue = "nonExistentUser";
    UpdateUserRequest request =
        new UpdateUserRequest("John", "Doe", "Institution", "10/10/2025", "leaner", "External",
            "Active", "Germany");
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(null);
    assertThrows(
        UserNotFoundException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidFirstName_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("", "Doe", "Institution", "10/10/2025", "leaner", "External",
            "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("First name must contain only alphabets and cannot be null or empty",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidLastName_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe123", "Institution", "10/10/2025", "leaner", "External",
            "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Last name must contain only alphabets and cannot be null or empty",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidInstitutionName_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "", "10/10/2025", "", "External", "Active","Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Institution name must contain only alphabets, numbers, or spaces and cannot be null or empty",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_MissingRole_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "10/10/2025", "", "External",
            "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Role is mandatory", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_CannotRemoveLearnerRole_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "10/10/2025", "invalidRole",
            "External", "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("cannot remove role 'learner'", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_ExpiryDateInThePast_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "01/01/2020", "learner", "External",
            "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);

    // Mock valid countries
    List<String> validCountries = List.of("Germany", "USA", "India");
    when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
        .map(LookupDto::new)
        .toList());
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals(
        "User account expiry date must be at least one day after today when status is 'Active'",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_ExpiryDateInThePastOrFutureWhenStatusIsInactive_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "01/01/2020", "learner", "External",
            "Inactive", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    // Mock valid countries
    List<String> validCountries = List.of("Germany", "USA", "India");
    when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
        .map(LookupDto::new)
        .toList());
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("User account expiry date must be the current date when status is 'Inactive'",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidUserType_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "01/01/2026", "learner", "InvalidType",
            "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("User type should be either 'Internal' or 'External' and cannot be null or empty",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidStatus_ThrowsValidationException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution", "01/01/2026", "learner", "External",
            "Invalid", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Status should be either 'Active' or 'Inactive' and cannot be null or empty",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_InvalidCountry_ThrowsException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request = new UpdateUserRequest(
        "John", "Doe", "Institution", "01/01/2026", "learner", "Internal", "Active", "InvalidCountry"
    );

    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));

    assertEquals("Invalid Country: InvalidCountry. Please provide a valid country name", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_deleteCognitoUser_Failure() {
    // Arrange
    User existingUser = new User();
    existingUser.setEmailId("john.doe@example.com");
    existingUser.setStatus("Inactive");

    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS); // Set loginOption to avoid null

    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    String partitionKeyValue = "user123";

    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    when(cognitoService.disableCognitoUserAsync(any(User.class))).thenReturn(
        CompletableFuture.failedFuture(new RuntimeException("Cognito disable failed")));

    UpdateUserRequest request = new UpdateUserRequest(
        "John",
        "Doe",
        "Institution",
        LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
        "learner",
        "External",
        "Inactive",
        "Germany"
    );

    // Mock valid countries
    List<String> validCountries = List.of("Germany", "USA", "India");
    when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
        .map(LookupDto::new)
        .toList());

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    // Verify interactions
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
    verify(userFilterSortDao, times(0)).updateUserByPk(any(User.class), anyString());
    verify(cognitoService, existingUser.getLoginOption() != null
        && existingUser.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)
        ? times(1) : times(0)).disableCognitoUserAsync(any(User.class));
  }

  @Test
  void updateUserByPk_deleteCognitoUser_Success() throws Exception {
    // Arrange
    User existingUser = new User();
    existingUser.setEmailId("test@example.com");
    existingUser.setStatus("Inactive");
    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS); // Initialize loginOption
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    String partitionKeyValue = "user123";

    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);
    when(cognitoService.deleteCognitoUserAsync(existingUser)).thenReturn(successFuture);

    // Act
    boolean isSuccess = Boolean.TRUE.equals(
        ReflectionTestUtils.invokeMethod(userServiceImpl, "deleteCognitoUser", existingUser));

    // Assert
    assertTrue(isSuccess, "Cognito user deletion should be successful");
    verify(cognitoService, times(1)).deleteCognitoUserAsync(existingUser);
  }

  @Test
  void updateUserByPk_handleNameChange_firstNameAndLastNameChanged_Success() throws Exception {
    User existingUser = new User();
    existingUser.setFirstName("John");
    existingUser.setLastName("Doe");
    existingUser.setEmailId("john@example.com");
    existingUser.setRole("content-author,learner");
    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS);
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    UpdateUserRequest updateUserRequest = new UpdateUserRequest();
    updateUserRequest.setFirstName("Johnny");
    updateUserRequest.setLastName("Does");
    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.updateCognitoUserAsync(any(User.class))).thenReturn(future);
    when(courseManagementServiceClient.updateCourseCreatedByName(anyString(), anyString()))
        .thenReturn("Success");
    ReflectionTestUtils.invokeMethod(userServiceImpl,
        "handleNameChange", existingUser, updateUserRequest);
    verify(cognitoService).updateCognitoUserAsync(any(User.class));
    verify(cognitoService, times(1)).updateCognitoUserAsync(any(User.class));
  }

  @Test
  void updateUserByPk_handleNameChange_UpdateFails_ThrowsException() throws Exception {
    User existingUser = new User();
    existingUser.setFirstName("John");
    existingUser.setLastName("Doe");
    existingUser.setEmailId("john@example.com");
    existingUser.setRole("learner");
    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS);
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");
    UpdateUserRequest updateUserRequest = new UpdateUserRequest();
    updateUserRequest.setFirstName("Johnny");
    updateUserRequest.setLastName("Doe");
    CompletableFuture<Void> future
        = CompletableFuture.failedFuture(new ExecutionException(new Throwable("Error")));
    when(cognitoService.updateCognitoUserAsync(any(User.class))).thenReturn(future);
    when(userFilterSortDao.getUserByPk("user123")).thenReturn(existingUser);
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> ReflectionTestUtils.invokeMethod(userServiceImpl, "handleNameChange",
            existingUser, updateUserRequest));
    assertEquals("Failed to update cognito user with email john@example.com",
        exception.getMessage());
    verify(userFilterSortDao, times(0)).updateUserByPk(any(User.class), anyString());
    verify(cognitoService, times(1)).updateCognitoUserAsync(any(User.class));
  }

  @Test
  void validateRole_ShouldThrowException_WhenRoleIsInvalid() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution",
            "10/10/2025", "learner, Invalidrole",
            "External", "Active", "Germany");
    User existingUser = new User();
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(existingUser);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Invalid Role: Invalidrole", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_validateRole_ShouldThrowException_WhenRoleIsNull() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution",
            "10/10/2025", null, "External",
            "Active", "Germany");
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(new User());
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("Role is mandatory", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_validateUserAccountExpiryDate_InvalidDate_ThrowsException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution",
            null, "learner", "External",
            "Active", "Germany");
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(new User());
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("User account expiry date cannot be null or empty", exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void updateUserByPk_validateUserExpiryDate_InvalidFormat_ThrowsException() {
    String partitionKeyValue = "user123";
    UpdateUserRequest request =
        new UpdateUserRequest("Jhon", "Doe", "Institution",
            "12-12-2024", "learner", "External",
            "Active", "Germany");
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(new User());
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userServiceImpl.updateUserByPk(partitionKeyValue, request));
    assertEquals("User account expiry date must be in the format MM/dd/yyyy",
        exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByPk(partitionKeyValue);
  }

  @Test
  void handleStatusUpdate_ShouldSetRoleToLearner_WhenStatusIsInactive() {
    // Arrange
    User existingUser = new User();
    existingUser.setRole("mentor");
    existingUser.setLoginOption(Constants.LOGIN_OPTION_LMS_CREDENTIALS); // Set loginOption to avoid null
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "dev");

    UpdateUserRequest updateUserRequest = new UpdateUserRequest();
    updateUserRequest.setStatus(Constants.IN_ACTIVE_STATUS);
    updateUserRequest.setUserAccountExpiryDate(LocalDate.now()
        .format(DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT)));

    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.disableCognitoUserAsync(existingUser)).thenReturn(future);

    // Act
    ReflectionTestUtils.invokeMethod(userServiceImpl, "handleStatusUpdate",
        existingUser, updateUserRequest);

    // Assert
    assertEquals(Constants.ROLE_LEARNER, existingUser.getRole());
  }

  @Test
  void testPersistLogFileData_ExceptionThrown() {
    OperationsHistoryDao operationsHistoryDao = mock(OperationsHistoryDao.class);
    ReflectionTestUtils.setField(userServiceImpl, "operationsHistoryDao", operationsHistoryDao);
    doThrow(new RuntimeException("Database error")).when(operationsHistoryDao)
        .saveLogFileData(any(OperationsHistory.class));
    String action = "UPLOAD";
    String fileName = "test.csv";
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ReflectionTestUtils.invokeMethod(userServiceImpl, "persistLogFileData", action, fileName);
    });
    assertEquals("Failed to persist log file data: Database error", exception.getMessage());
  }


  @Test
  @DisplayName("getAllUsers should return paginated users when all parameters are valid")
  void getAllUsers_ShouldReturnPaginatedUsers_WhenAllParametersAreValid() {
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "lms-admin";
    String institutionName = "institution";
    String searchValue = "search";
    UserListResponse expectedResponse = new UserListResponse();
    expectedResponse.setUserList(List.of(new User()));
    expectedResponse.setCount(1);
    int perPageInt = Integer.parseInt(perPage);
    when(userFilterSortDao.getUsers(sortKey, order, null, perPageInt, userRole,
        institutionName, searchValue, null))
        .thenReturn(expectedResponse);
    UserSummaryResponse actualResponse =
        userServiceImpl.getUserSummary(sortKey, order, null, perPage,
            userRole,
            institutionName, searchValue, null);
    assertEquals(1, actualResponse.getCount());
    verify(userFilterSortDao, times(1)).getUsers(sortKey, order, null,
        perPageInt, userRole, institutionName, searchValue, null);
  }

  @Test
  @DisplayName("getAllUsers should throw RuntimeError when an exception occurs")
  void getAllUsers_ShouldThrowUserNotFoundException_WhenExceptionOccurs() {
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "lms-admin";
    String institutionName = "institution";
    String searchValue = "search";
    int perPageInt = Integer.parseInt(perPage);
    when(userFilterSortDao.getUsers(sortKey, order, null, perPageInt, userRole,
        institutionName, searchValue, null))
        .thenThrow(new RuntimeException("Database error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.getUserSummary(sortKey, order, null, perPage, userRole,
          institutionName, searchValue, null);
    });
    assertEquals("Error fetching paginated users An internal error occurred. Please contact support.","Error fetching paginated users An internal error occurred. Please contact support.");
    verify(userFilterSortDao, times(1)).getUsers(sortKey, order, null,
        perPageInt, userRole, institutionName, searchValue, null);
  }

  @Test
  @DisplayName("getAllUsers should handle null searchValue")
  void getAllUsers_ShouldHandleNullSearchValue() {
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "lms-admin";
    String institutionName = "institution";
    int perPageInt = Integer.parseInt(perPage);
    UserListResponse expectedResponse = new UserListResponse();
    expectedResponse.setUserList(List.of(new User()));
    expectedResponse.setCount(1);

    when(userFilterSortDao.getUsers(sortKey, order, null, perPageInt, userRole,
        institutionName, null, null))
        .thenReturn(expectedResponse);

    UserSummaryResponse actualResponse =
        userServiceImpl.getUserSummary(sortKey, order, null, perPage,
            userRole,
            institutionName, null, null);
    assertEquals(1, actualResponse.getCount());
    verify(userFilterSortDao, times(1)).getUsers(sortKey, order, null,
        perPageInt, userRole, institutionName, null, null);
  }

  @Test
  @DisplayName("getAllUsers should handle searchValue without @")
  void getAllUsers_ShouldHandleSearchValueWithoutAtSymbol() {
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "lms-admin";
    String institutionName = "institution";
    String searchValue = "John Doe";
    String expectedSearchValue = "johndoe";
    int perPageInt = Integer.parseInt(perPage);
    UserListResponse expectedResponse = new UserListResponse();
    expectedResponse.setUserList(List.of(new User()));
    expectedResponse.setCount(1);
    when(userFilterSortDao.getUsers(sortKey, order, null, perPageInt, userRole,
        institutionName, expectedSearchValue, null))
        .thenReturn(expectedResponse);
    UserSummaryResponse actualResponse =
        userServiceImpl.getUserSummary(sortKey, order, null, perPage,
            userRole,
            institutionName, searchValue, null);
    assertEquals(1, actualResponse.getCount());
    verify(userFilterSortDao, times(1)).getUsers(sortKey, order, null,
        perPageInt, userRole, institutionName, expectedSearchValue, null);
  }

  @Test
  @DisplayName("scheduleUserExpiry should return NO_EXPIRED_USER_FOUND")
  void testScheduleUserExpiry_NoExpiredUsersFound() {
    when(userFilterSortDao.getExpiredUsers()).thenReturn(Set.of());
    String result = userServiceImpl.scheduleUserExpiry();
    assertEquals(Constants.NO_EXPIRED_USER_FOUND, result);
    verify(userFilterSortDao, times(1)).getExpiredUsers();
  }

  @Test
  @DisplayName("scheduleUserExpiry should return EXPIRED_USER_DEACTIVATED")
  void testScheduleUserExpiry_ExpiredUsersDeactivated() {
    Set<String> expiredEmails = Set.of("abc.xyz@cognizant.com", "qwerty@gamil.com");
    List<String> deactivatedEmails = List.of("abc.xyz@cognizant.com", "qwerty@gamil.com");
    when(userFilterSortDao.getExpiredUsers()).thenReturn(expiredEmails);
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    User user1 =
        new User(
            "abc", "xyz",
            "Cognizant", "abc.xyz@cognizant.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    User user2 =
        new User(
            "qwerty", "xyz",
            "Cognizant", "qwerty@gamil.com",
            "Internal", "learner, mentor",
            "12-12-2025","Y", "Skillspring Credentials");
    when(userFilterSortDao.getUserByEmailId("abc.xyz@cognizant.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("qwerty@gamil.com", "Active")).thenReturn(user2);
    when(userFilterSortDao.deactivateUser(user1, "lms-admin")).thenReturn(true);
    when(userFilterSortDao.deactivateUser(user2, "lms-admin")).thenReturn(true);
    String result = userServiceImpl.scheduleUserExpiry();
    assertEquals(Constants.EXPIRED_USER_DEACTIVATED, result);
    verify(userFilterSortDao, times(1)).getExpiredUsers();
    verify(userFilterSortDao, times(1)).getUserByEmailId("abc.xyz@cognizant.com", "Active");
    verify(userFilterSortDao, times(1)).getUserByEmailId("qwerty@gamil.com", "Active");
  }

  @Test
  @DisplayName("scheduleUserExpiry should throw RuntimeException when an error occurs")
  void testScheduleUserExpiry_ExceptionThrown() {
    when(userFilterSortDao.getExpiredUsers()).thenThrow(new RuntimeException("Database error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.scheduleUserExpiry();
    });
    assertEquals("Failed to deactivate users: Database error", exception.getMessage());
    verify(userFilterSortDao, times(1)).getExpiredUsers();
  }

  @Test
  @DisplayName("getUserByEmailId should return user when user exists")
  void getUserByEmailId_ShouldReturnUser_WhenUserExists() {
    String emailId = "test@example.com";
    String status = "Active";
    User expectedUser = new User();
    expectedUser.setEmailId(emailId);
    expectedUser.setStatus(status);

    when(userFilterSortDao.getUserByEmailId(emailId, status)).thenReturn(expectedUser);

    User actualUser = userServiceImpl.getUserByEmailId(emailId, status);

    assertNotNull(actualUser);
    assertEquals(expectedUser, actualUser);
    verify(userFilterSortDao, times(1)).getUserByEmailId(emailId, status);
  }

  @Test
  @DisplayName("getUserByEmailId should throw UserNotFoundException when user does not exist")
  void getUserByEmailId_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
    String emailId = "nonexistent@example.com";
    String status = "Active";

    when(userFilterSortDao.getUserByEmailId(emailId, status)).thenReturn(null);

    UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
      userServiceImpl.getUserByEmailId(emailId, status);
    });

    assertEquals("User not found in database with email: " + emailId, exception.getMessage());
    verify(userFilterSortDao, times(1)).getUserByEmailId(emailId, status);
  }

  @Test
  void getUserSummary_withValidParams_returnsUserSummary() {
    String sortKey = "sortKey";
    String order = "asc";
    String lastEvaluatedKeyEncoded = null;
    String perPage = "10";
    String userRole = "role";
    String institutionName = "institution";
    String searchValue = "search";
    String status = "Active";
    int perPageInt = Integer.parseInt(perPage);
    UserListResponse userListResponse = new UserListResponse();
    userListResponse.setUserList(List.of(new User(), new User()));
    userListResponse.setLastEvaluatedKey(null);
    userListResponse.setCount(2);
    when(userFilterSortDao.getUsers(sortKey, order, null,
        perPageInt, userRole, institutionName, searchValue, status))
        .thenReturn(userListResponse);
    UserSummaryResponse response = userServiceImpl.getUserSummary(sortKey, order,
        lastEvaluatedKeyEncoded, perPage, userRole, institutionName, searchValue, status);
    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatus());
    assertEquals(2, ((List<?>) response.getData()).size());
    assertEquals(2, response.getCount());
  }

  @Test
  void getUserSummary_withInvalidBase64Key_returnsBadRequest() {
    String sortKey = "sortKey";
    String order = "asc";
    String lastEvaluatedKeyEncoded = "invalidBase64";
    String perPage = "10";
    String userRole = "role";
    String institutionName = "institution";
    String searchValue = "search";
    String status = "Active";
    UserSummaryResponse response =
        userServiceImpl.getUserSummary(sortKey, order, lastEvaluatedKeyEncoded,
            perPage, userRole, institutionName, searchValue, status);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    assertEquals("Invalid Base64 encoded lastEvaluatedKey", response.getError());
  }

  @Test
  void getUserSummary_withShortSearchValue_returnsBadRequest() {
    String sortKey = "sortKey";
    String order = "asc";
    String lastEvaluatedKeyEncoded = null;
    String perPage = "10";
    String userRole = "role";
    String institutionName = "institution";
    String searchValue = "ab";
    String status = "Active";
    UserSummaryResponse response =
        userServiceImpl.getUserSummary(sortKey, order, lastEvaluatedKeyEncoded, perPage,
            userRole, institutionName, searchValue, status);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    assertEquals("Value must contain at least 3 characters", response.getError());
  }

  @Test
  void getUserSummary_withEmptyUserList_returnsOkWithErrorMessage() {
    String sortKey = "sortKey";
    String order = "asc";
    String lastEvaluatedKeyEncoded = null;
    String perPage = "10";
    String userRole = "role";
    String institutionName = "institution";
    String searchValue = "search";
    String status = "Active";
    int perPageInt = Integer.parseInt(perPage);
    UserListResponse userListResponse = new UserListResponse();
    userListResponse.setUserList(Collections.emptyList());
    userListResponse.setCount(0);
    when(userFilterSortDao.getUsers(sortKey, order, null, perPageInt,
        userRole, institutionName, searchValue, status))
        .thenReturn(userListResponse);
    UserSummaryResponse response =
        userServiceImpl.getUserSummary(sortKey, order, lastEvaluatedKeyEncoded, perPage,
            userRole, institutionName, searchValue, status);
    assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
    assertEquals("No users found", response.getError());
    assertEquals(0, ((List<?>) response.getData()).size());
  }

  @Test
  void getUserInstitutions_ShouldReturnInstitutions_WhenSortKeyIsValid() {
    String sortKey = "name";
    List<String> expectedInstitutions = List.of("Institution1", "Institution2");
    when(userFilterSortDao.getInstitutions(sortKey)).thenReturn(expectedInstitutions);
    List<String> actualInstitutions = userServiceImpl.getUserInstitutions(sortKey);
    assertEquals(expectedInstitutions, actualInstitutions);
  }

  @Test
  void getUserInstitutions_ShouldThrowRuntimeException_WhenDaoThrowsException() {
    String sortKey = "name";
    when(userFilterSortDao.getInstitutions(sortKey))
        .thenThrow(new RuntimeException("Database error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.getUserInstitutions(sortKey);
    });
    assertEquals("Database error", exception.getMessage());
  }
  @Test
  void registerLoggedInUser_ShouldReturnLoggedInUser() {
    when(authUser.getUsername()).thenReturn("testUser");
    when(authUser.getUserId()).thenReturn("Test123");
    when(authUser.getUserRoles()).thenReturn(List.of("role1", "role2"));
    when(authUser.getUserEmail()).thenReturn("test.user@example.com");

    User user = new User();
    user.setPk("Test123");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setUserType("Content-Author");
    user.setLoginOption("YES");
    user.setTermsAccepted("Testing");
    user.setTermsAcceptedDate("12-12-12");
    user.setPortal("portal");
    user.setTenantCode("243");
    TenantConfigDto tenantConfig = new TenantConfigDto();
    tenantConfig.setCookieDomain("abc");
    when(userFilterSortDao.getUserByEmailId("test.user@example.com", Constants.ACTIVE_STATUS))
        .thenReturn(user);
    when(teanatTableDao.fetchTenantConfig("t-2")).thenReturn(tenantConfig);

    LoggedInUser loggedInUser = userServiceImpl.registerLoggedInUser("testUser", "123" , "portal");

    assertNotNull(loggedInUser);
    assertEquals("testUser", loggedInUser.getName());
    assertEquals("Test123", loggedInUser.getUserId());
    assertEquals("test.user@example.com", loggedInUser.getUserEmail());
    assertEquals("User", loggedInUser.getLastName());
    assertEquals("Test", loggedInUser.getFirstName());
    assertEquals("portal", loggedInUser.getPortal());
  }



  @Test
  void testGetDownloadErrorLogFile_FileNotFound_Local() throws Exception {
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");
    String filename = "nonexistent.txt";
    String fileType = "txt";
    Exception exception = assertThrows(FileNotFoundException.class, () -> {
      userServiceImpl.getDownloadErrorLogFile(filename, fileType);
    });
    assertEquals("File not found: nonexistent.txt", exception.getMessage());
  }

  @Test
  void validUsers_shouldReturnValidUsers() {
    List<String> userEmails = List.of("user1@example.com", "user2@example.com");
    User user1 = new User();
    user1.setPk("1");
    user1.setEmailId("user1@example.com");
    User user2 = new User();
    user2.setPk("2");
    user2.setEmailId("user2@example.com");

    when(userFilterSortDao.getUserByEmailId("user1@example.com", "Active")).thenReturn(user1);
    when(userFilterSortDao.getUserByEmailId("user2@example.com", "Active")).thenReturn(user2);

    List<Map<String, String>> result = userServiceImpl.validUsers(userEmails);

    assertEquals(2, result.size());
    assertEquals("1", result.get(0).get("userId"));
    assertEquals("user1@example.com", result.get(0).get("email"));
    assertEquals("2", result.get(1).get("userId"));
    assertEquals("user2@example.com", result.get(1).get("email"));
  }

  @Test
  void validUsers_shouldReturnEmptyListWhenNoValidUsers() {
    List<String> userEmails = List.of("invalid1@example.com", "invalid2@example.com");

    when(userFilterSortDao.getUserByEmailId("invalid1@example.com", "Active")).thenReturn(null);
    when(userFilterSortDao.getUserByEmailId("invalid2@example.com", "Active")).thenReturn(null);

    List<Map<String, String>> result = userServiceImpl.validUsers(userEmails);

    assertEquals(0, result.size());
  }

  @Test
  void testRecreateUserInCognitoAndSendWelcomeEmail() {
    // Arrange
    String email = "test@example.com";
    User mockUser = new User();
    mockUser.setEmailId(email);
    ReflectionTestUtils.invokeMethod(userServiceImpl, "deleteCognitoUser", mockUser);
    when(userFilterSortDao.getUserByEmailId(email, Constants.ACTIVE_STATUS)).thenReturn(mockUser);

    // Act
    boolean result = userServiceImpl.recreateUserInCognitoAndSendWelcomeEmail(email);

    // Assert
    assertTrue(result, "The method should return true when the user is successfully recreated and welcome email is sent.");
    verify(userFilterSortDao, times(1)).getUserByEmailId(email, Constants.ACTIVE_STATUS);
  }



  @Test
  void testMigratePasswordChangedDate_NoUsersFound() {
    // Arrange
    when(userDao.getAllUsers()).thenReturn(List.of());

    // Act
    userServiceImpl.migratePasswordChangedDate();

    // Assert
    verify(userDao, times(1)).getAllUsers();
    verify(userDao, never()).createUser(any(User.class));
  }


  @Test
  void testMigratePasswordChangedDate_Success() {
    // Arrange
    User user1 = new User();
    user1.setPk("1");
    user1.setEmailId("user1@example.com");
    user1.setEmailId("user1@example.com");
    user1.setLastLoginTimestamp("2023-01-01");
    user1.setPasswordChangedDate(null);

    User user2 = new User();
    user2.setPk("2");
    user2.setEmailId("user2@example.com");
    user2.setEmailId("user2@example.com");
    user2.setLastLoginTimestamp("2023-02-01");
    user2.setPasswordChangedDate("2023-02-01");

    when(userDao.getAllUsers()).thenReturn(List.of(user1, user2));

    // Act
    userServiceImpl.migratePasswordChangedDate();

    // Assert
    verify(userDao, times(1)).getAllUsers();
    verify(userDao, times(1)).addPasswordChangedDate(user1.getPk(), user1.getSk(),"user1@example.com", "2023-01-01");

  }



  @Test
  void testMigratePasswordChangedDate_ExceptionFetchingUsers() {
    // Arrange
    when(userDao.getAllUsers()).thenThrow(new RuntimeException("Database error"));

    // Act
    userServiceImpl.migratePasswordChangedDate();

    // Assert
    verify(userDao, times(1)).getAllUsers();
  }

  @Test
  void testUpdateLastLoginTimeStampAndPasswordChangedDate_Success() {
    // Arrange
    UpdateDateDTO updateDateDTO = new UpdateDateDTO();
    updateDateDTO.setEmailId("user@example.com");
    updateDateDTO.setLastLoginTimestamp("2023-01-01");
    updateDateDTO.setPasswordChangedDate("2023-01-02");

    User user = new User();
    user.setEmailId("user@example.com");
    user.setLastLoginTimestamp("2022-12-31");
    user.setPasswordChangedDate("2022-12-30");

    when(userFilterSortDao.getUserByEmailId("user@example.com","Active")).thenReturn(user);

    // Act
    String result = userServiceImpl.updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO);

    // Assert
    assertEquals("User date fields updated successfully.", result);
    verify(userDao, times(1)).updateLastLoginTimeStampAndPasswordChangedDate(
        user.getPk(), user.getSk(), updateDateDTO
    );
  }

  @Test
  void testUpdateLastLoginTimeStampAndPasswordChangedDate_UserNotFound() {
    // Arrange
    UpdateDateDTO updateDateDTO = new UpdateDateDTO();
    updateDateDTO.setEmailId("nonexistent@example.com");

    when(userFilterSortDao.getUserByEmailId("nonexistent@example.com","Active")).thenReturn(null);

    // Act & Assert
    UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
      userServiceImpl.updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO);
    });
    assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());

  }
  @Test
  void testSetPasswordAndSendEmailAsync_Success() throws Exception {
    // Arrange
    String emailId = "user@example.com";
    User user = new User();
    user.setEmailId(emailId);

    when(userFilterSortDao.getUserByEmailId(emailId, "Active")).thenReturn(user);
    when(cognitoService.temporaryPasswordGenerator()).thenReturn("tempPassword");
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.setCognitoUserPasswordAsync(emailId, "tempPassword", user)).thenReturn(future);

    // Act
    boolean result = userServiceImpl.setPasswordAndSendEmailAsync(emailId);

    // Assert
    assertTrue(result);
    verify(cognitoService, times(1)).sendTemporaryPasswordEmail(user, "tempPassword");
  }

  @Test
  void testSetPasswordAndSendEmailAsync_UserNotFound() {
    // Arrange
    String emailId = "nonexistent@example.com";
    when(userFilterSortDao.getUserByEmailId(emailId, "Active")).thenReturn(null);

    // Act
    boolean result = userServiceImpl.setPasswordAndSendEmailAsync(emailId);

    // Assert
    assertFalse(result);
    verify(cognitoService, never()).temporaryPasswordGenerator();
  }

  @Test
  void testSetPasswordAndSendEmailAsync_TemporaryPasswordGenerationFails() {
    // Arrange
    String emailId = "user@example.com";
    User user = new User();
    user.setEmailId(emailId);

    when(userFilterSortDao.getUserByEmailId(emailId, "Active")).thenReturn(user);
    when(cognitoService.temporaryPasswordGenerator()).thenThrow(new RuntimeException("Error generating password"));

    // Act
    boolean result = userServiceImpl.setPasswordAndSendEmailAsync(emailId);

    // Assert
    assertFalse(result);
    verify(cognitoService, never()).setCognitoUserPasswordAsync(anyString(), anyString(), any());
  }

  @Test
  void testSetPasswordAndSendEmailAsync_SetPasswordFails() throws Exception {
    // Arrange
    String emailId = "user@example.com";
    User user = new User();
    user.setEmailId(emailId);

    when(userFilterSortDao.getUserByEmailId(emailId, "Active")).thenReturn(user);
    when(cognitoService.temporaryPasswordGenerator()).thenReturn("tempPassword");
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Error setting password"));
    when(cognitoService.setCognitoUserPasswordAsync(emailId, "tempPassword", user)).thenReturn(future);

    // Act
    boolean result = userServiceImpl.setPasswordAndSendEmailAsync(emailId);

    // Assert
    assertFalse(result);
    verify(cognitoService, never()).sendTemporaryPasswordEmail(any(), anyString());
  }

  @Test
  void updatePasswordChangedDate_ShouldUpdateSuccessfully() {
    // Arrange
    User user = new User();
    user.setPk("user123");
    user.setSk("userSk");
    user.setEmailId("test@example.com");

    // Act
    userServiceImpl.updatePasswordChangedDate(user);

    // Assert
    ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
    verify(userDao).addPasswordChangedDate(eq("user123"), eq("userSk"), eq("test@example.com"), dateCaptor.capture());

    String capturedDate = dateCaptor.getValue();
    assertDoesNotThrow(() -> Instant.parse(capturedDate)); // Use Instant.parse for validation
  }

  @Test
  void updatePasswordChangedDate_ShouldThrowException_WhenUserIsNull() {
    // Act & Assert
    assertThrows(NullPointerException.class, () -> userServiceImpl.updatePasswordChangedDate(null));
  }

  @Test
  void updatePasswordChangedDate_ShouldLogInfo_WhenExecutedSuccessfully() {
    // Arrange
    User user = new User();
    user.setPk("user123");
    user.setSk("userSk");
    user.setEmailId("test@example.com");

    // Act
    userServiceImpl.updatePasswordChangedDate(user);

    // Assert
    verify(userDao, times(1)).addPasswordChangedDate(eq("user123"), eq("userSk"), eq("test@example.com"), anyString());
    // Verify log message (if log capturing is set up in the test framework)
  }

  @Test
  void updatePasswordChangedDate_ShouldHandleDaoException() {
    // Arrange
    User user = new User();
    user.setPk("user123");
    user.setSk("userSk");
    user.setEmailId("test@example.com");

    doThrow(new RuntimeException("Database error")).when(userDao).addPasswordChangedDate(anyString(), anyString(), anyString(), anyString());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> userServiceImpl.updatePasswordChangedDate(user));
    verify(userDao, times(1)).addPasswordChangedDate(eq("user123"), eq("userSk"), eq("test@example.com"), anyString());
  }



  @Test
  @DisplayName("File validation fails")
  void testReActivateUsers_FileValidationFails() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "test data".getBytes(StandardCharsets.UTF_8));
    doThrow(new FileStorageException("Invalid file")).when(fileUtil).validateFile(file);

    FileStorageException exception = assertThrows(FileStorageException.class, () ->
        userServiceImpl.reActivateUsers(file, "Re-Activate Users")
    );

    assertEquals("Invalid file", exception.getMessage());
  }

  @Test
  @DisplayName("Full success")
  void testReActivateUsers_FullSuccess() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "test data".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");

    CSVProcessResponse csvResponse = new CSVProcessResponse();
    csvResponse.setSuccessCount(1);
    csvResponse.setFailureCount(0);
    csvResponse.setTotalCount(1);
    csvResponse.setValidUsers(Collections.singletonList(new User()));

    doNothing().when(fileUtil).validateFile(file);
    when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);
    when(csvProcessor.processReActivateUsers(file)).thenReturn(csvResponse);

    FileUploadResponse response = userServiceImpl.reActivateUsers(file, "Re-Activate Users");

    assertNotNull(response);
    assertTrue(response.getSuccessMessage().contains("processed successfully"));
    assertNull(response.getErrorLogFileName());
  }

  @Test
  @DisplayName("Partial success throws FileProcessingException")
  void testReActivateUsers_PartialSuccess_ThrowsException() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "test data".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "local");

    CSVProcessResponse csvResponse = new CSVProcessResponse();
    csvResponse.setSuccessCount(1);
    csvResponse.setFailureCount(1);
    csvResponse.setTotalCount(2);
    csvResponse.setValidUsers(Collections.singletonList(new User()));
    csvResponse.setErrors(Collections.emptyList());

    doNothing().when(fileUtil).validateFile(file);
    when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);
    when(csvProcessor.processReActivateUsers(file)).thenReturn(csvResponse);

    FileProcessingException exception = assertThrows(FileProcessingException.class, () ->
        userServiceImpl.reActivateUsers(file, "Re-Activate Users")
    );

    assertTrue(exception.getMessage().contains("processed partially"));
  }

  @Test
  @DisplayName("CSV processor returns null")
  void testReActivateUsers_CSVProcessorReturnsNull() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "test data".getBytes(StandardCharsets.UTF_8));

    doNothing().when(fileUtil).validateFile(file);
    when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);
    when(csvProcessor.processReActivateUsers(file)).thenReturn(null);

    NullPointerException exception = assertThrows(NullPointerException.class, () ->
        userServiceImpl.reActivateUsers(file, "Re-Activate Users")
    );

    assertNotNull(exception.getMessage());
  }

  @Test
  @DisplayName("Null file input throws exception")
  void testReActivateUsers_NullFile_ThrowsException() {
    MultipartFile file = null;

    NullPointerException exception = assertThrows(NullPointerException.class, () ->
        userServiceImpl.reActivateUsers(file, "Re-Activate Users")
    );

    assertNotNull(exception.getMessage());
  }

  @Test
  @DisplayName("Unsupported file type")
  void testReActivateUsers_UnsupportedFileType() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "invalid".getBytes(StandardCharsets.UTF_8));

    doThrow(new FileStorageException("Unsupported file type")).when(fileUtil).validateFile(file);

    FileStorageException exception = assertThrows(FileStorageException.class, () ->
        userServiceImpl.reActivateUsers(file, "Re-Activate Users")
    );

    assertEquals("Unsupported file type", exception.getMessage());
  }


  @Test
  void testHandleReactivationErrLogging_successfulErrorLogCreation_throwsFileProcessingException() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    CSVProcessResponse csvProcessResponse = mock(CSVProcessResponse.class);
    FileUploadResponse response = new FileUploadResponse();
    String errorLogFileName = "error_log.csv";
    String action = "REACTIVATE";

    List<String> errors = List.of("2025-08-18--Invalid Email--user@example.com--Row 2");
    when(csvProcessResponse.getErrors()).thenReturn(errors);
    when(csvProcessResponse.getFailureCount()).thenReturn(1);
    when(csvProcessResponse.getSuccessCount()).thenReturn(0);
    when(csvProcessResponse.getTotalCount()).thenReturn(1);
    when(file.getOriginalFilename()).thenReturn("users.csv");

    // Mock file saving to return true
    when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(true);

    // Use reflection to invoke the private method
    Method method = UserServiceImpl.class.getDeclaredMethod("handleReactivationErrLogging",
        MultipartFile.class, String.class, CSVProcessResponse.class, String.class, FileUploadResponse.class);
    method.setAccessible(true);

    try {
      method.invoke(userServiceImpl, file, action, csvProcessResponse, errorLogFileName, response);
      fail("Expected FileProcessingException to be thrown");
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertInstanceOf(FileProcessingException.class, cause);
      assertEquals("File users.csv failed with 1 errors.", cause.getMessage());
      assertEquals(errorLogFileName, ((FileProcessingException) cause).getFileUploadResponse().getErrorLogFileName());
    }
  }

  @Test
  void testHandleReactivationErrLogging_errorLogCreationFails_throwsFileStorageException() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    CSVProcessResponse csvProcessResponse = mock(CSVProcessResponse.class);
    FileUploadResponse response = new FileUploadResponse();
    String errorLogFileName = "error_log.csv";
    String action = "REACTIVATE";

    List<String> errors = List.of("2025-08-18--Invalid Email--user@example.com--Row 2");
    when(csvProcessResponse.getErrors()).thenReturn(errors);
    when(csvProcessResponse.getFailureCount()).thenReturn(1);
    when(csvProcessResponse.getSuccessCount()).thenReturn(0);
    when(csvProcessResponse.getTotalCount()).thenReturn(1);
    when(file.getOriginalFilename()).thenReturn("users.csv");

    // Mock file saving to return false
    when(fileUtil.saveFileToLocal(any(), anyString(), anyString())).thenReturn(false);

    // Use reflection to invoke the private method
    Method method = UserServiceImpl.class.getDeclaredMethod("handleReactivationErrLogging",
        MultipartFile.class, String.class, CSVProcessResponse.class, String.class, FileUploadResponse.class);
    method.setAccessible(true);

    try {
      method.invoke(userServiceImpl, file, action, csvProcessResponse, errorLogFileName, response);
      fail("Expected FileStorageException to be thrown");
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertTrue(cause instanceof FileStorageException);
      assertEquals("Failed to save file : " + errorLogFileName, cause.getMessage());
    }
  }

  @Test
  void testCreateUserForCognizantSSO_shouldCreateUserWithExpectedFields() {
    String firstName = "John";
    String lastName = "Doe";
    String email = "John.Doe@cognizant.com";

    UserServiceImpl spyService = Mockito.spy(userServiceImpl);

    // Stub createUser to return the same user passed in
    doAnswer(invocation -> invocation.getArgument(0)).when(spyService).createUser(any(User.class));

    User result = spyService.createUserForCognizantSSO(firstName, lastName, email);

    assertNotNull(result);
    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getLastName());
    assertEquals("john.doe@cognizant.com", result.getEmailId());
    assertEquals(Constants.USER_TYPE_INTERNAL, result.getUserType());
    assertEquals(Constants.ROLE_LEARNER, result.getRole());
    assertEquals(Constants.LOGIN_OPTION_COGNIZANT_SSO, result.getLoginOption());
    assertEquals("John Doe", result.getCreatedBy());
    assertNotNull(result.getCreatedOn());
  }


  @Test
  void testCreateUserForCognizantSSO_shouldConvertEmailToLowercase() {
    String email = "TEST.USER@COGNIZANT.COM";

    UserServiceImpl spyService = Mockito.spy(userServiceImpl);
    doAnswer(invocation -> invocation.getArgument(0)).when(spyService).createUser(any(User.class));

    User result = spyService.createUserForCognizantSSO("Test", "User", email);

    assertEquals("test.user@cognizant.com", result.getEmailId());
  }

  @Test
  void testCreateUserForCognizantSSO_shouldSetCreatedOnInExpectedFormat() {
    String firstName = "Alice";
    String lastName = "Smith";
    String email = "alice.smith@cognizant.com";

    UserServiceImpl spyService = Mockito.spy(userServiceImpl);
    doAnswer(invocation -> invocation.getArgument(0)).when(spyService).createUser(any(User.class));

    User result = spyService.createUserForCognizantSSO(firstName, lastName, email);

    assertNotNull(result.getCreatedOn());

    // Match the format used in the actual method
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    assertDoesNotThrow(() -> LocalDateTime.parse(result.getCreatedOn(), formatter));
  }

  @Test
  @DisplayName("updateTermsAcceptedByPartitionKeyValue - Success")
  void updateTermsAcceptedByPartitionKeyValue_Success() {
    // Given
    String partitionKeyValue = "user123";
    User mockUser = new User();
    mockUser.setPk("user123");
    mockUser.setSk("sk123");
    mockUser.setName("testuser");

    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(mockUser);
    when(userFilterSortDao.updateUserTermsAccepted(eq("user123"), eq("sk123"), eq("Y"), anyString())).thenReturn(true);

    // When
    userServiceImpl.updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    // Then
    verify(userFilterSortDao).getUserByPk(partitionKeyValue);
    verify(userFilterSortDao).updateUserTermsAccepted(eq("user123"), eq("sk123"), eq("Y"), anyString());
  }

  @Test
  @DisplayName("updateTermsAcceptedByPartitionKeyValue - User not found")
  void updateTermsAcceptedByPartitionKeyValue_UserNotFound() {
    // Given
    String partitionKeyValue = "nonexistent";
    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(null);

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
        () -> userServiceImpl.updateTermsAcceptedByPartitionKeyValue(partitionKeyValue));

    assertEquals("User not found with partitionKey: " + partitionKeyValue, exception.getMessage());
    verify(userFilterSortDao).getUserByPk(partitionKeyValue);
    verify(userFilterSortDao, never()).updateUserTermsAccepted(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("updateTermsAcceptedByPartitionKeyValue - Update fails")
  void updateTermsAcceptedByPartitionKeyValue_UpdateFails() {
    // Given
    String partitionKeyValue = "user123";
    User mockUser = new User();
    mockUser.setPk("user123");
    mockUser.setSk("sk123");
    mockUser.setName("testuser");

    when(userFilterSortDao.getUserByPk(partitionKeyValue)).thenReturn(mockUser);
    when(userFilterSortDao.updateUserTermsAccepted(eq("user123"), eq("sk123"), eq("Y"), anyString())).thenReturn(false);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> userServiceImpl.updateTermsAcceptedByPartitionKeyValue(partitionKeyValue));

    assertEquals("Failed to update terms accepted for user: testuser", exception.getMessage());
    verify(userFilterSortDao).getUserByPk(partitionKeyValue);
    verify(userFilterSortDao).updateUserTermsAccepted(eq("user123"), eq("sk123"), eq("Y"), anyString());
  }

  // Test cases for updateTermsAccepted method
  @Test
  @DisplayName("updateTermsAccepted - Success")
  void updateTermsAccepted_Success() {
    // Given
    when(userFilterSortDao.updateTermsAccepted()).thenReturn(true);

    // When
    userServiceImpl.updateTermsAccepted();

    // Then
    verify(userFilterSortDao).updateTermsAccepted();
  }

  @Test
  @DisplayName("updateTermsAccepted - Update fails")
  void updateTermsAccepted_UpdateFails() {
    // Given
    when(userFilterSortDao.updateTermsAccepted()).thenReturn(false);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> userServiceImpl.updateTermsAccepted());

    assertEquals("Failed to update terms accepted for user: ", exception.getMessage());
    verify(userFilterSortDao).updateTermsAccepted();
  }

  @Test
  @DisplayName("deleteBulkUsers - Successfully deletes users from Cognito and DB")
  void testDeleteBulkUsers_Success() {
    MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
        "email\nuser1@example.com\nuser2@example.com".getBytes(StandardCharsets.UTF_8));

    List<User> mockUsers = List.of(
        User.builder().pk("user#1").emailId("user1@example.com").build(),
        User.builder().pk("user#2").emailId("user2@example.com").build()
    );

    when(csvProcessor.processDeleteBulkUsers(file, 1, 100)).thenReturn(mockUsers);

    String result = userServiceImpl.deleteBulkUsers(file, 1, 100);

    assertEquals("Deleted users successfully", result);
    verify(csvProcessor).processDeleteBulkUsers(file, 1, 100);
    verify(userFilterSortDao).deleteBatchUsers(mockUsers);
  }



  @Test
  @DisplayName("deleteBulkUsers - Verifies Cognito deletion per user")
  void testDeleteBulkUsers_VerifyCognitoCalls() {
    MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
        "email\nuser1@example.com\nuser2@example.com".getBytes(StandardCharsets.UTF_8));

    List<User> mockUsers = List.of(
        User.builder().pk("user#1").emailId("user1@example.com").build(),
        User.builder().pk("user#2").emailId("user2@example.com").build()
    );

    when(csvProcessor.processDeleteBulkUsers(file, 1, 100)).thenReturn(mockUsers);

    UserServiceImpl spyService = Mockito.spy(new UserServiceImpl(
            userDao, userFilterSortDao, cognitoService, s3Utils, csvProcessor,
            fileUtil, roleDao, lookupDao, applicationEnv, bucketName, storageBucket,
            logFileDao, courseManagementServiceClient, userManagementEventPublisherService,
            csvValidator, userActivityLogService, userActivityLogDao, teanatTableDao, "rootDomainPath"
    ));

    doReturn(true).when(spyService).deleteCognitoUser(any(User.class));

    when(cognitoService.deleteBulkCognitoUserAsync(any(User.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    String result = spyService.deleteBulkUsers(file, 1, 100);

    assertEquals("Deleted users successfully", result);
    verify(cognitoService, times(2)).deleteBulkCognitoUserAsync(any(User.class));
    verify(userFilterSortDao).deleteBatchUsers(mockUsers);
  }

  @Test
  @DisplayName("deleteBulkUsers - No users to delete")
  void testDeleteBulkUsers_EmptyList() {
    MultipartFile file = new MockMultipartFile(
        "file",
        "users.csv",
        "text/csv",
        "".getBytes(StandardCharsets.UTF_8)
    );

    when(csvProcessor.processDeleteBulkUsers(file, 1, 100)).thenReturn(Collections.emptyList());

    String result = userServiceImpl.deleteBulkUsers(file, 1, 100);

    assertEquals("Deleted users successfully", result);
    verify(userFilterSortDao).deleteBatchUsers(Collections.emptyList());
  }

  @Test
  @DisplayName("deleteBulkUsers - CSV processing throws exception")
  void testDeleteBulkUsers_CsvProcessingFails() {
    MultipartFile file = new MockMultipartFile(
        "file",
        "users.csv",
        "text/csv",
        "bad data".getBytes(StandardCharsets.UTF_8)
    );

    when(csvProcessor.processDeleteBulkUsers(file, 1, 100))
        .thenThrow(new RuntimeException("CSV error"));

    RuntimeException ex = assertThrows(RuntimeException.class, () -> {
      userServiceImpl.deleteBulkUsers(file, 1, 100);
    });

    assertEquals("CSV error", ex.getMessage());
  }

  @Test
  @DisplayName("uploadBulkUsers - success with valid CSV and headers")
  void testUploadBulkUsers_Success() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
        "firstName,lastName,emailId\nJohn,Doe,john@example.com".getBytes(StandardCharsets.ISO_8859_1));

    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "prod");

    doNothing().when(fileUtil).validateFile(file);
    when(s3Utils.saveFileToS3(any(), anyString(), anyString())).thenReturn(true);
    when(csvValidator.validateHeaders(any(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(true);
    userManagementEventPublisherService = mock(UserManagementEventPublisherService.class);
    ReflectionTestUtils.setField(userServiceImpl, "userManagementEventPublisherService", userManagementEventPublisherService);
    doNothing().when(userManagementEventPublisherService).triggerAddUserPublishEvent(any());

    FileUploadResponse result = userServiceImpl.uploadBulkUsers(file, "Add Users");

    assertEquals("File uploaded successfully", result.getSuccessMessage());
    verify(userManagementEventPublisherService).triggerAddUserPublishEvent(any());
  }

  @Test
  @DisplayName("uploadBulkUsers - fails due to IOException during CSV parsing")
  void testUploadBulkUsers_IOException() throws Exception {
    MultipartFile file = mock(MultipartFile.class);

    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "prod");

    when(file.getOriginalFilename()).thenReturn("users.csv");
    when(file.getInputStream()).thenThrow(new IOException("Stream error"));
    doNothing().when(fileUtil).validateFile(file);
    when(s3Utils.saveFileToS3(any(), anyString(), anyString())).thenReturn(true);

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        userServiceImpl.uploadBulkUsers(file, "Add Users"));

    assertEquals("Error processing CSV file", ex.getMessage());
  }


  @Test
  @DisplayName("uploadBulkUsers - fails due to invalid CSV headers")
  void testUploadBulkUsers_InvalidHeaders() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
        "wrongHeader1,wrongHeader2\nvalue1,value2".getBytes(StandardCharsets.ISO_8859_1));

    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "prod");

    doNothing().when(fileUtil).validateFile(file);
    when(s3Utils.saveFileToS3(any(), anyString(), anyString())).thenReturn(true);
    when(csvValidator.validateHeaders(any(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(false);

    FileProcessingException ex = assertThrows(FileProcessingException.class, () ->
        userServiceImpl.uploadBulkUsers(file, "Add Users"));

    assertEquals("File users.csv failed with 0 errors.", ex.getMessage());
  }

  @Test
  @DisplayName("uploadBulkUsers - fails when error log file cannot be saved (indirect)")
  void testUploadBulkUsers_ErrorLogSaveFails_Indirect() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
        "wrongHeader1,wrongHeader2\nvalue1,value2".getBytes(StandardCharsets.ISO_8859_1));

    ReflectionTestUtils.setField(userServiceImpl, "applicationEnv", "prod");

    doNothing().when(fileUtil).validateFile(file);
    when(s3Utils.saveFileToS3(any(), anyString(), anyString())).thenReturn(true);
    when(csvValidator.validateHeaders(any(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(false);

    // Simulate failure by mocking logFileDao to throw
    doThrow(new RuntimeException("Failed to save error log file")).when(logFileDao).saveLogFileData(any());

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        userServiceImpl.uploadBulkUsers(file, "Add Users"));

    assertTrue(ex.getMessage().contains("Failed to persist log file data"));
  }

  @Test
  void updatePreferredUI_ShouldUpdateSuccessfully() {
    String userId = "user123";
    String preferredUI = "modern";
    User user = new User();
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(user);

    String result = userServiceImpl.updatePreferredUI(userId, preferredUI);

    assertEquals("Preferred UI updated successfully to modern", result);
    verify(userFilterSortDao).getUserByPk(userId);
    verify(userFilterSortDao).updateLearnerPreferredView(user, preferredUI);
  }

  @Test
  void updatePreferredUI_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
    String userId = "user123";
    String preferredUI = "modern";
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(null);

    assertThrows(UserNotFoundException.class, () -> userServiceImpl.updatePreferredUI(userId, preferredUI));
    verify(userFilterSortDao).getUserByPk(userId);
  }

  @Test
  void updateIsWatchedTutorial_ShouldUpdate_WhenUserExists() {
    String userId = "user123";
    User user = new User();
    user.setPk(userId);
    user.setSk("sk1");
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(user);
    when(userFilterSortDao.updateIsWatchedTutorial(userId, "sk1")).thenReturn(true);

    userServiceImpl.updateIsWatchedTutorial(userId);

    verify(userFilterSortDao).getUserByPk(userId);
    verify(userFilterSortDao).updateIsWatchedTutorial(userId, "sk1");
  }

  @Test
  void updateIsWatchedTutorial_ShouldThrow_WhenUserNotFound() {
    String userId = "user123";
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(null);

    assertThrows(UserNotFoundException.class, () -> userServiceImpl.updateIsWatchedTutorial(userId));
    verify(userFilterSortDao).getUserByPk(userId);
  }

  @Test
  void recordVideoLaunch_ShouldUpdate_WhenUserExists() {
    String userId = "user123";
    User user = new User();
    user.setPk(userId);
    user.setSk("sk1");
    user.setName("Test User");
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(user);
    when(userFilterSortDao.updateVideoLaunchCount(userId, "sk1")).thenReturn(true);

    userServiceImpl.recordVideoLaunch(userId);

    verify(userFilterSortDao).getUserByPk(userId);
    verify(userFilterSortDao).updateVideoLaunchCount(userId, "sk1");
  }

  @Test
  void recordVideoLaunch_ShouldThrow_WhenUserNotFound() {
    String userId = "user123";
    when(userFilterSortDao.getUserByPk(userId)).thenReturn(null);

    assertThrows(UserNotFoundException.class, () -> userServiceImpl.recordVideoLaunch(userId));
    verify(userFilterSortDao).getUserByPk(userId);
  }

  @Test
  @DisplayName("Test addAdminUser creates superadmin successfully")
  void testAddAdminUser_Success_Superadmin() throws Exception {
    String tenantCode = "t-2";
    String role = "superadmin";
    User user = new User();
    user.setEmailId("admin@cognizant.com");
    user.setFirstName("Admin");
    user.setLastName("User");


    TenantConfigDto tenantConfig = new TenantConfigDto();
    tenantConfig.setUserPoolId("us-east-1_yYbihycOA");
    when(teanatTableDao.fetchTenantConfig(tenantCode)).thenReturn(tenantConfig);
    when(userFilterSortDao.getUserByEmailIdAndTenant(user.getEmailId(), Constants.ACTIVE_STATUS, tenantCode)).thenReturn(null);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    when(cognitoService.createUserWithPoolIdAsync(any(User.class), eq(tenantConfig.getUserPoolId()))).thenReturn(future);
    doNothing().when(userDao).createUser(any(User.class));
      System.out.println("InstitutionName: " + user.getInstitutionName());
      System.out.println("Role: " + user.getRole());
    boolean result = userServiceImpl.addAdminUser(user, role, tenantCode);
      System.out.println("InstitutionName: " + user.getInstitutionName());
      System.out.println("Role: " + user.getRole());

    assertTrue(result);
    assertEquals("Cognizant", user.getInstitutionName());
    assertEquals(Constants.SUPER_ADMIN_ROLE+",learner", user.getRole());
    verify(teanatTableDao).fetchTenantConfig(tenantCode);
    verify(cognitoService).createUserWithPoolIdAsync(any(User.class), eq(tenantConfig.getUserPoolId()));
    verify(userDao).createUser(any(User.class));
  }


}