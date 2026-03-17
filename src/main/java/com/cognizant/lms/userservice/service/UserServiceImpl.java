package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.client.CourseManagementServiceClient;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.constants.ProcessConstants;


import com.cognizant.lms.userservice.dao.*;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.domain.User;

import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.FileProcessingException;
import com.cognizant.lms.userservice.exception.FileStorageException;
import com.cognizant.lms.userservice.exception.UserNotFoundException;
import com.cognizant.lms.userservice.exception.ValidationException;
import com.cognizant.lms.userservice.exception.CognitoServiceException;
import com.cognizant.lms.userservice.utils.*;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


@Service
@Slf4j
public class UserServiceImpl implements UserService {
  private static final String PASSWORD_POLICY_REGEX =
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+.]).{8,}$";

  private final UserDao userDao;
  private final CognitoAsyncService cognitoService;
  private final OperationsHistoryDao operationsHistoryDao;
  private final UserFilterSortDao userFilterSortDao;
  private final String applicationEnv;
  private final String bucketName;
  private final String localStoragePath;
  private final S3Util s3Utils;
  private final CourseManagementServiceClient courseManagementServiceClient;
  private final UserManagementEventPublisherService userManagementEventPublisherService;
  private final UserActivityLogService userActivityLogService;
  private final UserActivityLogDao userActivityLogDao;
  private final TeanatTableDao teanatTableDao;

  private CSVProcessor csvProcessor;
  private CSVValidator csvValidator;

  private FileUtil fileUtil;

  private RoleDao roleDao;

  private LookupDao lookupDao;

  private final String rootDomainPath;

  @Value("${AWS_COGNITO_CLIENT_ID:}")
  private String awsCognitoClientId;

  @Value("${AWS_COGNITO_CLIENT_SECRET:}")
  private String awsCognitoClientSecret;

  public UserServiceImpl(UserDao userDao,
                         UserFilterSortDao userFilterSortDao, CognitoAsyncService cognitoService,
                         S3Util s3Utils, CSVProcessor csvProcessor, FileUtil fileUtil,
                         RoleDao roleDao,
                         LookupDao lookupDao,
                         @Value("${APP_ENV}") String applicationEnv,
                         @Value("${AWS_S3_BUCKET_NAME}") String bucketName,
                         @Value("${LOCAL_STORAGE_PATH}") String localStoragePath,
                         OperationsHistoryDao operationsHistoryDao,
                         CourseManagementServiceClient courseManagementServiceClient,
                         UserManagementEventPublisherService userManagementEventPublisherService,
                         CSVValidator csvValidator,
                         UserActivityLogService userActivityLogService,
                         UserActivityLogDao userActivityLogDao,
                         TeanatTableDao teanatTableDao,
                         @Value("${ROOT_DOMAIN_PATH}") String rootDomainPath) {
    this.userDao = userDao;
    this.userFilterSortDao = userFilterSortDao;
    this.cognitoService = cognitoService;
    this.s3Utils = s3Utils;
    this.csvProcessor = csvProcessor;
    this.fileUtil = fileUtil;
    this.roleDao = roleDao;
    this.lookupDao = lookupDao;
    this.applicationEnv = applicationEnv;
    this.bucketName = bucketName;
    this.localStoragePath = localStoragePath;
    this.operationsHistoryDao = operationsHistoryDao;
    this.courseManagementServiceClient = courseManagementServiceClient;
    this.userManagementEventPublisherService = userManagementEventPublisherService;
    this.csvValidator = csvValidator;
    this.userActivityLogService = userActivityLogService;
    this.userActivityLogDao = userActivityLogDao;
    this.teanatTableDao = teanatTableDao;
    this.rootDomainPath = rootDomainPath;
  }

  public User createUser(User user) {
    try {
      user.setPk(generateUniqueId());
      user.setSk(generateUniqueId());
      String institutionName =
          user.getInstitutionName().isEmpty() ? "Cognizant" : user.getInstitutionName();
      user.setInstitutionName(institutionName);
      user.setGsiSortFNLN(user.getFirstName() + Constants.HASH + user.getLastName());
      user.setName(user.getFirstName().toLowerCase().replace(" ", "")
          + user.getLastName().toLowerCase().replace("   ", ""));
      user.setStatus(Constants.ACTIVE_STATUS);
      if (user.getUserAccountExpiryDate() != null
          && !user.getUserAccountExpiryDate().trim().isEmpty()) {
        DateTimeFormatter inputFormatter =
            DateTimeFormatter.ofPattern(Constants.FIRST_AS_MONTH_FORMAT);
        DateTimeFormatter outputFormatter =
            DateTimeFormatter.ofPattern(Constants.FIRST_AS_YEAR_FORMAT);
        LocalDate expiryDate =
            LocalDate.parse(user.getUserAccountExpiryDate().trim(), inputFormatter);
        String formattedExpiryDate = expiryDate.format(outputFormatter);
        user.setUserAccountExpiryDate(formattedExpiryDate);
      } else {
        user.setUserAccountExpiryDate(Constants.USER_DEFAULT_EXPIRY_DATE);
      }
      user.setCreatedOn(user.getCreatedOn());
      String roleStr = user.getRole();
      if (roleStr == null || roleStr.isEmpty()) {
        user.setRole(Constants.ROLE_LEARNER);
      } else if (!roleStr.contains(Constants.ROLE_LEARNER)) {
        user.setRole(roleStr.toLowerCase() + Constants.COMA + Constants.ROLE_LEARNER);
      } else {
        user.setRole(roleStr.toLowerCase());
      }
      if (user.getTenantCode() == null || user.getTenantCode().isEmpty()) {
          user.setTenantCode(TenantUtil.getTenantCode());
      }
      user.setViewOnlyAssignedCourses(user.getViewOnlyAssignedCourses());

      //Set the country field
      String country = user.getCountry();
      if (country == null || country.trim().isEmpty()) {
        country = Constants.DEFAULT_COUNTRY;
      }
      user.setCountry(country.trim());

      // Set termsAccepted to "N" for external users
      if (user.getUserType() != null && user.getUserType().equals(Constants.USER_TYPE_EXTERNAL)) {
        user.setTermsAccepted("N");
      }
      //setting passwordchangedate to current date
      ZonedDateTime currentDateTime = ZonedDateTime.now();
      String formattedDateTime = currentDateTime.format(DateTimeFormatter.ISO_INSTANT);
      user.setPasswordChangedDate(formattedDateTime);
      user.setPreferredUI(Constants.PREFERRED_UI_CLASSIC);
      userDao.createUser(user);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException(
          "Unable to add User " + user.getPk() + " with error : " + e.getMessage());
    }
    return user;
  }

  private static final Random random = new Random();

  public static String getRandomYN() {
    return random.nextBoolean() ? Constants.YES : Constants.NO;
  }

  @Override
  public UserSummaryResponse getUserSummary(String sortKey, String order,
                                            String lastEvaluatedKeyEncoded,
                                            String perPage, String userRole, String institutionName,
                                            String searchValue, String status) {
    try {
    int perPageInt = Integer.parseInt(perPage);
    if (perPageInt <= 0) {
      log.error("Invalid perPage value: %s".formatted(perPage));
      UserSummaryResponse response = new UserSummaryResponse();
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("Invalid perPage value. Please provide number greater than zero.");
      return response;
    }
    UserSummaryResponse response = new UserSummaryResponse();
    Map<String, String> lastEvaluatedKey = null;
    if (lastEvaluatedKeyEncoded != null) {
      try {
        lastEvaluatedKey = Base64Util.decodeEvaluatedKey(lastEvaluatedKeyEncoded);
      } catch (IllegalArgumentException e) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Invalid Base64 encoded lastEvaluatedKey");
        return response;
      }
    }
    if (searchValue != null && searchValue.length() < 3) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("Value must contain at least 3 characters");
      return response;
    }
    UserListResponse result =
        getAllUsers(sortKey, order, lastEvaluatedKey, perPageInt, userRole,
            institutionName, searchValue, status);
    List<User> userList = result.getUserList();
    Map<String, AttributeValue> lastKey = result.getLastEvaluatedKey();
    int totalCount = result.getCount();
    if (userList.isEmpty()) {
      response.setData(new ArrayList<User>());
      response.setStatus(HttpStatus.NO_CONTENT.value());
      response.setError(Constants.ERROR_MESSAGE);
    } else {
      response.setData(userList);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      response.setCount(totalCount);
      log.info("lastkey {}", lastKey);
      if (lastKey != null && !lastKey.isEmpty()) {
        response.setLastEvaluatedKey(Base64Util.encodeLastEvaluatedKey(lastKey));
      }
    }
    log.info("Fetching users summary for {} users", userList.size());
    return response;
    } catch (NumberFormatException e) {
      log.error("Invalid perPage value: %s".formatted(perPage));
      UserSummaryResponse response = new UserSummaryResponse();
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("Invalid perPage value. Please provide a valid number.");
      return response;
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  private UserListResponse getAllUsers(String sortKey, String order,
                                       Map<String, String> lastEvaluatedKey,
                                       int perPage, String userRole, String institutionName,
                                       String searchValue, String status) {
    UserListResponse response;
    try {
      if (searchValue != null && !searchValue.contains("@")) {
        searchValue = searchValue.toLowerCase().replace(" ", "");
      }
      response = userFilterSortDao.getUsers(sortKey, order, lastEvaluatedKey, perPage,
          userRole, institutionName, searchValue, status);
      log.info("Fetching Paginated Users from db");
    } catch (Exception e) {
      log.error("Failed to fetch all the Paginated users {} ", e.getMessage());
      throw new RuntimeException("Error fetching paginated users internal error occurred. Please contact support.");
    }
    return response;
  }

  @Override
  public FileUploadResponse uploadUsers(MultipartFile file, String action) throws Exception {
    FileUploadResponse response = new FileUploadResponse();
    String fileResponse = uploadBulkUsersFile(file);
    if (fileResponse.contains(Constants.FILE_SAVED_MESSAGE)) {
      CSVProcessResponse csvProcessResponse = csvProcessor.processAddUserFile(file);
      String errorLogFileName = generateErrorLogFileName(file);
      String createdBy = UserContext.getCreatedBy();
      if (csvProcessResponse.getSuccessCount() != 0 && isProcessingSuccessful(csvProcessResponse)) {
        handleUsersUploading(csvProcessResponse, response, file, createdBy);
      } else if (csvProcessResponse.getSuccessCount() != 0 && csvProcessResponse.getFailureCount()
          != 0 && isPartialProcessing(csvProcessResponse)) {
        handleUsersUploadingAndErrLogging(file, action, csvProcessResponse, errorLogFileName,
            response, createdBy);
      } else {
        handleErrLogging(file, action, csvProcessResponse, errorLogFileName,
            response);
      }
    }
    return response;
  }

  private void handleUsersUploading(CSVProcessResponse csvProcessResponse,
                                    FileUploadResponse response,
                                    MultipartFile file, String createdBy) {
    log.info("Uploading {} users", csvProcessResponse.getValidUsers().size());
    List<User> persistedCognitoUsers =
        persistUserDataAndCreateCognitoForUsers(csvProcessResponse.getValidUsers(), createdBy);
    if (persistedCognitoUsers.isEmpty()) {
      log.info("No valid user data found to upload to the database. Please check the csv"
          + " file and try again.");
      throw new FileProcessingException("No valid user data found to upload to the database."
          + " Please check the csv file and try again.");
    }
    log.info("Uploaded successfully {} users data to the database.",
        persistedCognitoUsers.size());
    response.setSuccessMessage("File " + file.getOriginalFilename()
        + " processed successfully with " + csvProcessResponse.getTotalCount()
        + " records verified and 0 errors.");
  }

  private void handleUsersUploadingAndErrLogging(MultipartFile file, String action,
                                                 CSVProcessResponse csvProcessResponse,
                                                 String errorLogFileName,
                                                 FileUploadResponse response,
                                                 String createdBy)
      throws Exception {
    if (csvProcessResponse.getSuccessCount() != 0) {
      log.info("handling error while uploading {} users to the database", csvProcessResponse.getValidUsers().size());
      List<User> persistedCognitoUsers =
          persistUserDataAndCreateCognitoForUsers(csvProcessResponse.getValidUsers(), createdBy);
      if (persistedCognitoUsers.isEmpty()) {
        log.info("No valid user data found to upload to the database."
            + " Please check the csv file and try again.");
        throw new FileProcessingException("No valid user data found to upload to the database."
            + " Please check the csv file and try again.");
      }
      log.info("Uploaded {} users data to the database.", persistedCognitoUsers.size());
    }
    log.info("Error list {}",
        csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForUploadUsers(csvProcessResponse.getErrors().stream().distinct().toList(),
            errorLogFileName, file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException(
          "Failed to save file : " + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException(
        "File " + file.getOriginalFilename() + " processed partially with "
            + csvProcessResponse.getSuccessCount() + " records verified and "
            + csvProcessResponse.getFailureCount() + " errors.", response);
  }

  private void handleErrLogging(MultipartFile file, String action,
                                CSVProcessResponse csvProcessResponse,
                                String errorLogFileName,
                                FileUploadResponse response)
      throws Exception {
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForUploadUsers(csvProcessResponse.getErrors().stream().distinct().toList(),
            errorLogFileName, file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException(
          "Failed to save file : " + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException("File " + file.getOriginalFilename() + " failed with "
        + csvProcessResponse.getFailureCount() + " errors.", response);
  }

  private boolean saveErrorLogFileForUploadUsers(List<String> errors, String logFileName,
                                                 String originalFileName,
                                                 int failureCount, int successCount, int totalCount,
                                                 String action)
      throws IOException {
    String[][] errorData = new String[errors.size()][Constants.uploadUsersLogHeaders.size()];
    String fileUploadTime = null;
    for (int i = 0; i < errors.size(); i++) {
      String[] err = errors.get(i).split("--");
      fileUploadTime = err[0];
      errorData[i][0] = err[0];
      errorData[i][1] = err.length > 2 ? err[1] : " ";
      errorData[i][2] = err.length > 2 ? err[2] : " ";
      errorData[i][3] = err.length > 2 ? err[3] : " ";
      errorData[i][4] = err.length > 2 ? err[4] : err[1];
    }
    MultipartFile errorLogFile =
        createErrorLogFile(Constants.uploadUsersLogHeaders.toArray(new String[0]), errorData,
            fileUploadTime,
            originalFileName,
            logFileName, failureCount, successCount, totalCount, action);
    boolean isSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
        ?
        fileUtil.saveFileToLocal(errorLogFile, localStoragePath,
            Constants.LOCAL_DISK_PREFIX_ERRORLOG)
        : s3Utils.saveFileToS3(errorLogFile, bucketName,
        TenantUtil.getTenantCode() + Constants.ERROR_LOG_PREFIX + logFileName);

    if (isSaved) {
      persistLogFileData(action, logFileName);
    }
    return isSaved;
  }

  private boolean saveErrorLogFileForUploadBulkUsers(List<String> errors, String logFileName,
                                                 String originalFileName,
                                                 int failureCount, int successCount, int totalCount,
                                                 String action)
      throws IOException {
    String[][] errorData = new String[errors.size()][Constants.uploadUsersLogHeaders.size()];
    String fileUploadTime = null;
    for (int i = 0; i < errors.size(); i++) {
      String[] err = errors.get(i).split("--");
      fileUploadTime = err[0];
      errorData[i][0] = err[0];
      errorData[i][1] = err.length > 2 ? err[1] : " ";
      errorData[i][2] = err.length > 2 ? err[2] : " ";
      errorData[i][3] = err.length > 2 ? err[3] : " ";
      errorData[i][4] = err.length > 2 ? err[4] : err[1];
    }
    MultipartFile errorLogFile =
        createErrorLogFile(Constants.uploadUsersLogHeaders.toArray(new String[0]), errorData,
            fileUploadTime,
            originalFileName,
            logFileName, failureCount, successCount, totalCount, action);
    boolean isSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
        ?
        fileUtil.saveFileToLocal(errorLogFile, localStoragePath,
            Constants.LOCAL_DISK_PREFIX_ERRORLOG)
        : s3Utils.saveFileToS3(errorLogFile, bucketName,
        TenantUtil.getTenantCode() + Constants.ERROR_LOG_PREFIX + logFileName);
    return isSaved;
  }

  private MultipartFile createErrorLogFile(String[] logsHeaders,
                                           String[][] errorData, String fileUploadTime,
                                           String originalFileName,
                                           String logFileName,
                                           int failureCount, int successCount, int totalCount,
                                           String action) throws IOException {

    Column[] columns = new Column[logsHeaders.length];
    for (int i = 0; i < logsHeaders.length; i++) {
      columns[i] = new Column().header(logsHeaders[i]).headerAlign(HorizontalAlign.LEFT)
          .dataAlign(HorizontalAlign.LEFT);
    }
    String table = AsciiTable.getTable(AsciiTable.NO_BORDERS, columns, errorData);
    String multiLineError =
        String.format(Constants.MULTILINE_ERROR_LOG_FORMAT, originalFileName,
            fileUploadTime, totalCount, successCount, failureCount, action);
    List<String> errorLogs = new ArrayList<>();
    errorLogs.add(multiLineError);
    errorLogs.add(table);
    return new CustomMultipartFile(errorLogs, logFileName);
  }

  @Override
  public TenantDTO getTenantDetails(String tenantIdentifier) {
    return userDao.getTenantDetails(tenantIdentifier);
  }

  @Override
  public EmailSelfRegistrationResponse registerUserByEmail(EmailSelfRegistrationRequest request) {
    validateSelfRegistrationRequest(request);

    String tenantCode = TenantUtil.getTenantCode();
    TenantConfigDto tenantConfig = teanatTableDao.fetchTenantConfig(tenantCode);
    if (tenantConfig == null) {
      throw new ValidationException("Invalid tenant configuration");
    }

    String normalizedEmail = request.getEmail().trim().toLowerCase();
    User activeUser =
        userFilterSortDao.getUserByEmailIdAndTenant(normalizedEmail, Constants.ACTIVE_STATUS,
            tenantCode);
    User inActiveUser =
        userFilterSortDao.getUserByEmailIdAndTenant(normalizedEmail, Constants.IN_ACTIVE_STATUS,
            tenantCode);
    if (activeUser != null || inActiveUser != null) {
      throw new ValidationException("EmailId already exists");
    }

    String appClientId = resolveAppClientId(tenantConfig);
    String appClientSecret = resolveAppClientSecret(tenantConfig, appClientId);

    if (appClientId == null || appClientId.trim().isEmpty()) {
      throw new ValidationException("Tenant app client configuration is missing");
    }
    try {
      cognitoService.signUpUserAsync(
          normalizedEmail,
          request.getPassword().trim(),
          request.getFirstName().trim(),
          request.getLastName().trim(),
          appClientId.trim(),
          appClientSecret).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to register user in identity provider");
    } catch (ExecutionException e) {
      throw mapCognitoExecutionException(e, "Failed to register user in identity provider");
    }

    User user = new User(
        request.getFirstName().trim(),
        request.getLastName().trim(),
        request.getInstitute().trim(),
        normalizedEmail,
        Constants.USER_TYPE_EXTERNAL,
        Constants.ROLE_LEARNER,
        null,
        Constants.NO,
        Constants.LOGIN_OPTION_LMS_CREDENTIALS);

    user.setCreatedBy(request.getFirstName().trim() + " " + request.getLastName().trim());
    user.setTenantCode(tenantCode);
    user.setCountry(request.getCountry().trim());

    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    user.setCreatedOn(utcDateTime.format(formatter));

    User createdUser = createUser(user);
    return new EmailSelfRegistrationResponse(createdUser.getPk(), true);
  }

  @Override
  public EmailOtpActionResponse verifyRegistrationOtp(EmailOtpVerificationRequest request) {
    validateVerifyOtpRequest(request);

    String tenantCode = TenantUtil.getTenantCode();
    TenantConfigDto tenantConfig = teanatTableDao.fetchTenantConfig(tenantCode);
    if (tenantConfig == null) {
      throw new ValidationException("Invalid tenant configuration");
    }
    String appClientId = resolveAppClientId(tenantConfig);
    String appClientSecret = resolveAppClientSecret(tenantConfig, appClientId);

    if (appClientId == null || appClientId.trim().isEmpty()) {
      throw new ValidationException("Tenant app client configuration is missing");
    }

    String normalizedEmail = request.getEmail().trim().toLowerCase();
    try {
      cognitoService.confirmSignUpAsync(
          normalizedEmail,
          request.getOtp().trim(),
          appClientId.trim(),
          appClientSecret).get();
      return new EmailOtpActionResponse(normalizedEmail, true,
          "Email verification successful");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to verify OTP in identity provider");
    } catch (ExecutionException e) {
      throw mapCognitoExecutionException(e, "Failed to verify OTP in identity provider");
    }
  }

  @Override
  public EmailOtpActionResponse resendRegistrationOtp(EmailOtpResendRequest request) {
    validateResendOtpRequest(request);

    String tenantCode = TenantUtil.getTenantCode();
    TenantConfigDto tenantConfig = teanatTableDao.fetchTenantConfig(tenantCode);
    if (tenantConfig == null) {
      throw new ValidationException("Invalid tenant configuration");
    }
    String appClientId = resolveAppClientId(tenantConfig);
    String appClientSecret = resolveAppClientSecret(tenantConfig, appClientId);

    if (appClientId == null || appClientId.trim().isEmpty()) {
      throw new ValidationException("Tenant app client configuration is missing");
    }

    String normalizedEmail = request.getEmail().trim().toLowerCase();
    try {
      cognitoService.resendSignUpOtpAsync(
          normalizedEmail,
          appClientId.trim(),
          appClientSecret).get();
      return new EmailOtpActionResponse(normalizedEmail, true,
          "OTP has been sent to your email");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to resend OTP in identity provider");
    } catch (ExecutionException e) {
      throw mapCognitoExecutionException(e, "Failed to resend OTP in identity provider");
    }
  }

  private void validateSelfRegistrationRequest(EmailSelfRegistrationRequest request) {
    if (request == null) {
      throw new ValidationException("Request body is required");
    }

    if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
      throw new ValidationException("First name is required");
    }
    if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
      throw new ValidationException("Last name is required");
    }
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
      throw new ValidationException("Email is required");
    }
    if (!request.getEmail().trim().matches(Constants.EMAIL_PATTERN)) {
      throw new ValidationException("Invalid email format");
    }
    if (request.getCountry() == null || request.getCountry().trim().isEmpty()) {
      throw new ValidationException("Country is required");
    }
    if (request.getInstitute() == null || request.getInstitute().trim().isEmpty()) {
      throw new ValidationException("Institute is required");
    }
    if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
      throw new ValidationException("Password is required");
    }
    if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
      throw new ValidationException("Confirm password is required");
    }
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new ValidationException("Password and confirm password do not match");
    }
    if (!request.getPassword().matches(PASSWORD_POLICY_REGEX)) {
      throw new ValidationException(
          "Password must be at least 8 characters and include uppercase, lowercase, number, and special character");
    }

    String idpPreferences = TenantUtil.getTenantDetails().getIdpPreferences();
    if (idpPreferences == null || idpPreferences.trim().isEmpty()) {
      log.warn("Tenant identity provider preferences are not configured; allowing email self registration by default");
      return;
    }

    List<String> configuredLoginOptions = Arrays.stream(idpPreferences.toLowerCase().split(","))
        .map(String::trim)
        .toList();

    if (!configuredLoginOptions.contains(Constants.LOGIN_OPTION_LMS_CREDENTIALS.toLowerCase())) {
      throw new ValidationException("Email registration is not enabled for this tenant");
    }
  }

  private void validateVerifyOtpRequest(EmailOtpVerificationRequest request) {
    if (request == null) {
      throw new ValidationException("Request body is required");
    }
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
      throw new ValidationException("Email is required");
    }
    if (!request.getEmail().trim().matches(Constants.EMAIL_PATTERN)) {
      throw new ValidationException("Invalid email format");
    }
    if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
      throw new ValidationException("OTP is required");
    }
  }

  private void validateResendOtpRequest(EmailOtpResendRequest request) {
    if (request == null) {
      throw new ValidationException("Request body is required");
    }
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
      throw new ValidationException("Email is required");
    }
    if (!request.getEmail().trim().matches(Constants.EMAIL_PATTERN)) {
      throw new ValidationException("Invalid email format");
    }
  }

  private RuntimeException mapCognitoExecutionException(ExecutionException executionException,
                                                        String defaultMessage) {
    Throwable cause = executionException.getCause();
    if (cause instanceof CognitoServiceException cognitoServiceException) {
      String errorCode = cognitoServiceException.getErrorCode();
      if ("UsernameExistsException".equalsIgnoreCase(errorCode)) {
        return new ValidationException("EmailId already exists");
      }
      if ("CodeMismatchException".equalsIgnoreCase(errorCode)) {
        return new ValidationException("Invalid OTP");
      }
      if ("ExpiredCodeException".equalsIgnoreCase(errorCode)) {
        return new ValidationException("OTP has expired");
      }
      if ("UserNotFoundException".equalsIgnoreCase(errorCode)) {
        return new ValidationException("User not found in identity provider");
      }
      if ("InvalidParameterException".equalsIgnoreCase(errorCode)) {
        return new ValidationException(cognitoServiceException.getMessage());
      }
      return new ValidationException(cognitoServiceException.getMessage());
    }
    return new RuntimeException(defaultMessage);
  }

  private String resolveAppClientId(TenantConfigDto tenantConfig) {
    if (tenantConfig.getUserPoolAppId() != null && !tenantConfig.getUserPoolAppId().trim().isEmpty()) {
      return tenantConfig.getUserPoolAppId().trim();
    }
    if (awsCognitoClientId != null && !awsCognitoClientId.trim().isEmpty()) {
      return awsCognitoClientId.trim();
    }
    if (tenantConfig.getAppClientId() != null && !tenantConfig.getAppClientId().trim().isEmpty()) {
      return tenantConfig.getAppClientId().trim();
    }
    return null;
  }

  private String resolveAppClientSecret(TenantConfigDto tenantConfig, String resolvedAppClientId) {
    if (tenantConfig.getUserPoolAppSecret() != null
        && !tenantConfig.getUserPoolAppSecret().trim().isEmpty()) {
      return tenantConfig.getUserPoolAppSecret().trim();
    }
    if (awsCognitoClientSecret != null
        && !awsCognitoClientSecret.trim().isEmpty()
        && awsCognitoClientId != null
        && !awsCognitoClientId.trim().isEmpty()
        && resolvedAppClientId != null
        && resolvedAppClientId.trim().equals(awsCognitoClientId.trim())) {
      return awsCognitoClientSecret.trim();
    }
    return null;
  }

  @Override
  public User createUserForCognizantSSO(String firstName, String lastName, String email) {
    log.info("Creating Cognizant SSO user");
    User user =
        new User(firstName,
            lastName,
            "",
            email.toLowerCase(),
            Constants.USER_TYPE_INTERNAL,
            Constants.ROLE_LEARNER,
            null,
            "",
            Constants.LOGIN_OPTION_COGNIZANT_SSO);

    user.setCreatedBy(firstName + " " + lastName);

    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String createdOn = utcDateTime.format(formatter);
    user.setCreatedOn(createdOn);
    User createdUser = createUser(user);
    log.info("Cognizant SSO user created successfully");
    return createdUser;
  }

  private List<User> persistUserDataAndCreateCognitoForUsers(List<User> validUsers,
                                                             String createdBy) {
    List<User> persistedUsers = new ArrayList<>();

    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String createdOn = utcDateTime.format(formatter);
    for (User user : validUsers) {
      user.setEmailId(user.getEmailId().toLowerCase());
      if (!user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS) || createCognitoUser(user)) {
        user.setCreatedOn(createdOn);
        user.setCreatedBy(createdBy);
        User persistedData = createUser(user);
        if (!user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
          cognitoService.sendWelcomeMailToSocialIDP(user);
        }
        log.info("User created in database {}", persistedData.getEmailId());
        persistedUsers.add(persistedData);
      } else {
        log.error("Failed to create user in cognito {}", user.getEmailId());
        throw new RuntimeException("Failed to create user in cognito " + user.getEmailId());
      }
    }
    return persistedUsers;
  }

  @Override
  public Resource getDownloadErrorLogFile(String filename, String fileType) throws Exception {
    Path filePath;

    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      if (Constants.FILE_TYPE_TXT.equalsIgnoreCase(fileType)) {
        filePath = Paths.get(localStoragePath + Constants.LOCAL_DISK_PREFIX_ERRORLOG)
            .resolve(filename).normalize();
      } else if (Constants.FILE_TYPE_CSV.equalsIgnoreCase(fileType)) {
        filePath = Paths.get(localStoragePath + Constants.LOCAL_DISK_PREFIX_USERDATA)
            .resolve(filename).normalize();
      } else {
        throw new IllegalArgumentException("Unsupported file type: " + fileType);
      }
      if (!Files.exists(filePath)) {
        log.error("File not found: {}", filename);
        throw new FileNotFoundException("File not found: " + filename);
      }
      return new InputStreamResource(Files.newInputStream(filePath));
    } else {
      try {
        return s3Utils.downloadFileFromS3(filename, fileType, bucketName, false);
      } catch (Exception e) {
        log.error("Error downloading file from S3: {}", e.getMessage());
        throw new Exception("Error downloading file from S3");
      }
    }
  }

  public String uploadBulkUsersFile(MultipartFile file) throws Exception {
    log.info("Uploading file {} for bulk user creation", file.getOriginalFilename());
    fileUtil.validateFile(file);
    boolean isFileSaved = false;
    String storageLocationMsg = "";
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      isFileSaved =
          fileUtil.saveFileToLocal(file, localStoragePath, Constants.LOCAL_DISK_PREFIX_USERDATA);
      storageLocationMsg = Constants.storedInLocalMsg;
    } else {
      isFileSaved = s3Utils.saveFileToS3(file, bucketName,
          TenantUtil.getTenantCode() + Constants.S3_PREFIX + file.getOriginalFilename());
      storageLocationMsg = Constants.storedInS3Msg;
    }
    if (!isFileSaved) {
      throw new FileStorageException("Failed to save file" + file.getOriginalFilename());
    }
    return storageLocationMsg;
  }


  private String generateUniqueId() {
    return UUID.randomUUID().toString();
  }

  private boolean createCognitoUser(User user) {
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      log.warn("Cognito user creation is not supported in local environment");
      return true;
    } else {
      try {
        CompletableFuture<Void> future = cognitoService.createCognitoUserAsync(user);
        future.get();
        return true;
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }

  }

//  private Map<String, String> convertStringToMap(String str) {
//    Map<String, String> map = new HashMap<>();
//    String[] entries = str.substring(1, str.length() - 1).split(", ");
//    for (String entry : entries) {
//      String[] keyValue = entry.split("=");
//      map.put(keyValue[0], keyValue[1]);
//    }
//    return map;
//  }

  @Override
  public List<String> getUserInstitutions(String sortKey) {
    List<String> institutions;
    try {
      institutions = userFilterSortDao.getInstitutions(sortKey);
      log.info("Institutions from GSI: {}", institutions);
    } catch (Exception e) {
      log.error("Failed to fetch all the Institutions {} ", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
    return institutions;
  }

  @Override
  public FileUploadResponse reActivateUsers(MultipartFile file, String action) throws Exception {
    fileUtil.validateFile(file);
    FileUploadResponse response = new FileUploadResponse();
    String fileResponse = uploadBulkUsersFile(file);
    if (fileResponse.contains(Constants.FILE_SAVED_MESSAGE)) {
      CSVProcessResponse csvProcessResponse = csvProcessor.processReActivateUsers(file);
      String errorLogFileName = generateErrorLogFileName(file);
      String modifiedBy = UserContext.getModifiedBy();
      if (csvProcessResponse.getSuccessCount() != 0 && isProcessingSuccessful(csvProcessResponse)) {
        handleUserReActivation(file, csvProcessResponse, response);
      } else if (csvProcessResponse.getSuccessCount() != 0 && csvProcessResponse.getFailureCount()
          != 0 && isPartialProcessing(csvProcessResponse)) {
        handleUserReactivationAndErrLogging(file, action, csvProcessResponse,
            errorLogFileName, response, modifiedBy);
      } else {
        handleReactivationErrLogging(file, action, csvProcessResponse,
            errorLogFileName, response);
      }
    }
    return response;
  }

  @Override
  public FileUploadResponse deactivateUser(MultipartFile file, String action) throws Exception {
    FileUploadResponse response = new FileUploadResponse();
    String fileResponse = uploadBulkUsersFile(file);
    if (fileResponse.contains(Constants.FILE_SAVED_MESSAGE)) {
      CSVProcessResponse csvProcessResponse = csvProcessor.processDeActiveUserFile(file);
      String errorLogFileName = generateErrorLogFileName(file);
      String modifiedBy = UserContext.getModifiedBy();
      if (csvProcessResponse.getSuccessCount() != 0 && isProcessingSuccessful(csvProcessResponse)) {
        handleUsersDeactivation(file, csvProcessResponse, response, modifiedBy);
      } else if (csvProcessResponse.getSuccessCount() != 0 && csvProcessResponse.getFailureCount()
          != 0 && isPartialProcessing(csvProcessResponse)) {
        handleUsersDeactivationAndErrLogging(file, action, csvProcessResponse,
            errorLogFileName, response, modifiedBy);
      } else {
        handleDeactivationErrLogging(file, action, csvProcessResponse,
            errorLogFileName, response);
      }
    }
    return response;
  }


  private String generateErrorLogFileName(MultipartFile file) {
    return file.getOriginalFilename()
        + Constants.LOG_FILE_SUFFIX
        + LocalDateTime.now().format(DateTimeFormatter
        .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
        + Constants.LOG_FILE_EXTENSION;
  }

  private boolean isProcessingSuccessful(CSVProcessResponse csvProcessResponse) {
    return csvProcessResponse.getFailureCount() == 0
        && csvProcessResponse.getSuccessCount() == csvProcessResponse.getTotalCount();
  }

  private boolean isPartialProcessing(CSVProcessResponse csvProcessResponse) {
    return csvProcessResponse.getSuccessCount() + csvProcessResponse.getFailureCount()
        == csvProcessResponse.getTotalCount();
  }

  private void handleUserReActivation(MultipartFile file, CSVProcessResponse csvProcessResponse,
                                      FileUploadResponse response) {

    List<String> reactiveEmails = reactivateUserAndCreateCognitoForUsers(csvProcessResponse.getValidUsers());
    if (reactiveEmails.isEmpty()) {
      log.info("No valid email found to activate in the database ");
      throw new FileProcessingException("No user email found to reactivate in the database.");
    }
    response.setSuccessMessage("File " + file.getOriginalFilename() + " processed successfully with  "
        + csvProcessResponse.getTotalCount() + "  records verified and 0 errors.");
  }

  private void handleUsersDeactivation(MultipartFile file,
                                       CSVProcessResponse csvProcessResponse,
                                       FileUploadResponse response, String modifiedBy) {
    log.info("Deactivation started for {} emails", csvProcessResponse.getValidEmail().size());
    List<String> deletedEmails =
        deactivateUserDataAndDeleteCognitoForUsers(csvProcessResponse.getValidEmail(),
            modifiedBy);
    if (deletedEmails.isEmpty()) {
      log.info(
          "No valid user email found to deactivate in the database."
              + " Please check the csv file and try again.");
      throw new FileProcessingException(
          "No valid user email found to deactivate in the database."
              + " Please check the csv file and try again.");
    }
    response.setSuccessMessage(
        "File " + file.getOriginalFilename() + " processed successfully with "
            + csvProcessResponse.getTotalCount() + " records verified and 0 errors.");
  }

  private void handleUserReactivationAndErrLogging(MultipartFile file, String action,
                                                   CSVProcessResponse csvProcessResponse,
                                                   String errorLogFileName,
                                                   FileUploadResponse response, String modifiedBy) throws Exception {
    if (csvProcessResponse.getSuccessCount() != 0) {
      log.info("Handling errors while reactivating {} emails", csvProcessResponse.getValidUsers().size());
      List<String> reactivatedEmails =
          reactivateUserAndCreateCognitoForUsers(csvProcessResponse.getValidUsers());
      if (reactivatedEmails.isEmpty()) {
        log.error(
            "No valid user email found to reactivate in the database.");
        throw new FileProcessingException(
            "No valid user data email to reactivate in the database.");
      }
    }
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForReactivateusers(
            csvProcessResponse.getErrors().stream().distinct().toList(), errorLogFileName,
            file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException("Failed to save file : "
          + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException(
        "File " + file.getOriginalFilename() + " processed partially with "
            + csvProcessResponse.getSuccessCount() + " records verified and "
            + csvProcessResponse.getFailureCount() + " errors.", response);

  }

  private void handleUsersDeactivationAndErrLogging(MultipartFile file, String action,
                                                    CSVProcessResponse csvProcessResponse,
                                                    String errorLogFileName,
                                                    FileUploadResponse response, String modifiedBy)
      throws Exception {
    if (csvProcessResponse.getSuccessCount() != 0) {
      log.info("Handling errors while deactivating {} emails", csvProcessResponse.getValidEmail().size());
      List<String> deletedEmails =
          deactivateUserDataAndDeleteCognitoForUsers(csvProcessResponse.getValidEmail(),
              modifiedBy);
      if (deletedEmails.isEmpty()) {
        log.error(
            "No valid user email found to deactivate in the database."
                + " Please check the csv file and try again.");
        throw new FileProcessingException(
            "No valid user data email to deactivate in the database."
                + " Please check the csv file and try again.");
      }
    }
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForDeactivateUsers(
            csvProcessResponse.getErrors().stream().distinct().toList(), errorLogFileName,
            file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException("Failed to save file : "
          + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException(
        "File " + file.getOriginalFilename() + " processed partially with "
            + csvProcessResponse.getSuccessCount() + " records verified and "
            + csvProcessResponse.getFailureCount() + " errors.", response);
  }

  private void handleReactivationErrLogging(MultipartFile file, String action,
                                            CSVProcessResponse csvProcessResponse,
                                            String errorLogFileName,
                                            FileUploadResponse response) throws Exception {
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForReactivateusers(
            csvProcessResponse.getErrors().stream().distinct().toList(), errorLogFileName,
            file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException("Failed to save file : " + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException("File " + file.getOriginalFilename() + " failed with "
        + csvProcessResponse.getFailureCount() + " errors.", response);
  }

  private void handleDeactivationErrLogging(MultipartFile file, String action,
                                            CSVProcessResponse csvProcessResponse,
                                            String errorLogFileName,
                                            FileUploadResponse response)
      throws Exception {
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForDeactivateUsers(
            csvProcessResponse.getErrors().stream().distinct().toList(), errorLogFileName,
            file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException("Failed to save file : " + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException("File " + file.getOriginalFilename() + " failed with "
        + csvProcessResponse.getFailureCount() + " errors.", response);
  }

  private boolean saveErrorLogFileForReactivateusers(List<String> errors, String logFileName,
                                                     String originalFileName,
                                                     int failureCount, int successCount,
                                                     int totalCount,
                                                     String action) throws IOException {
    String[][] errorData = new String[errors.size()][Constants.REACTIVEUSER_LOG_HEADER.size()];
    String fileUploadTime = null;
    for (int i = 0; i < errors.size(); i++) {
      String[] err = errors.get(i).split("--");
      log.info("Error data {}", err[1]);

      fileUploadTime = err.length > 0 ? err[0] : " ";
      errorData[i][0] = fileUploadTime;
      errorData[i][1] = err.length > 2 ? err[1] : " ";
      errorData[i][2] = err.length > 2 ? err[2] : " ";
      errorData[i][3] = err.length > 2 ? err[3] : err[1];
    }
    MultipartFile errorLogFile =
        createErrorLogFile(Constants.REACTIVEUSER_LOG_HEADER
                .toArray(new String[0]), errorData,
            fileUploadTime,
            originalFileName, logFileName, failureCount, successCount, totalCount, action);
    boolean isSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
        ?
        fileUtil.saveFileToLocal(errorLogFile, localStoragePath,
            Constants.LOCAL_DISK_PREFIX_ERRORLOG)
        : s3Utils.saveFileToS3(errorLogFile, bucketName, TenantUtil.getTenantCode()
        + Constants.ERROR_LOG_PREFIX + logFileName);

    if (isSaved) {
      persistLogFileData(action, logFileName);
    }
    return isSaved;
  }

  private boolean saveErrorLogFileForDeactivateUsers(List<String> errors, String logFileName,
                                                     String originalFileName,
                                                     int failureCount, int successCount,
                                                     int totalCount,
                                                     String action)
      throws IOException {
    String[][] errorData = new String[errors.size()][Constants.deactivateUsersLogHeaders.size()];
    String fileUploadTime = null;
    for (int i = 0; i < errors.size(); i++) {
      String[] err = errors.get(i).split("--");
      log.info("Error data {}", err[1]);
      fileUploadTime = err[0];
      errorData[i][0] = err[0];
      errorData[i][1] = err.length > 2 ? err[1] : " ";
      errorData[i][2] = err.length > 2 ? err[2] : err[1];
    }
    MultipartFile errorLogFile =
        createErrorLogFile(Constants.deactivateUsersLogHeaders
                .toArray(new String[0]), errorData,
            fileUploadTime,
            originalFileName, logFileName, failureCount, successCount, totalCount, action);
    boolean isSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
        ?
        fileUtil.saveFileToLocal(errorLogFile, localStoragePath,
            Constants.LOCAL_DISK_PREFIX_ERRORLOG)
        : s3Utils.saveFileToS3(errorLogFile, bucketName, TenantUtil.getTenantCode()
        + Constants.ERROR_LOG_PREFIX + logFileName);

    if (isSaved) {
      persistLogFileData(action, logFileName);
    }
    return isSaved;
  }

  private List<String> reactivateUserAndCreateCognitoForUsers(List<User> validUsers) {
    List<String> reactivatedEmails = new ArrayList<>();
    validUsers.forEach(user -> {

      boolean success = handleReactivationcsv(user);
      if (success) {
        userFilterSortDao.reActivateUser(user, user.getUserAccountExpiryDate());
        reactivatedEmails.add(user.getEmailId());
      } else {
        log.info("Reactivation Fails for user {}", user);
      }
    });
    return reactivatedEmails;
  }

  private List<String> deactivateUserDataAndDeleteCognitoForUsers(Set<String> validEmail,
                                                                  String modifiedBy) {
    List<String> deletedEmails = new ArrayList<>();

    validEmail.forEach(email -> {
      User user = userFilterSortDao.getUserByEmailId(email.toLowerCase(), Constants.ACTIVE_STATUS);
      if (user == null) {
        log.info("User does not exist with email or user status is already inActive {}", email);
      } else {
        if (user.getLoginOption() != null && user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
          try {
            disableCognitoUser(user);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        if (userFilterSortDao.deactivateUser(user, modifiedBy)) {
          deletedEmails.add(email);
        } else {
          log.error("Failed to deactivate user with email {}", email);
        }
      }
    });

    return deletedEmails;
  }

  public boolean deleteCognitoUser(User user) {
    log.info("delete cognitoUser");
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      log.warn("Cognito user deletion is not supported in local environment");
      return true;
    } else if (user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
      try {
        CompletableFuture<Void> future = cognitoService.deleteCognitoUserAsync(user);
        future.get();
        return true;
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    } else {
      log.warn("Cognito user deletion is not supported for this login option");
      throw new RuntimeException("Cognito user deletion is not supported for this login option");

    }

  }

  @Override
  public FileUploadResponse updateUsers(MultipartFile file, String action) throws Exception {
    FileUploadResponse response = new FileUploadResponse();
    String fileResponse = uploadBulkUsersFile(file);
    if (fileResponse.contains(Constants.FILE_SAVED_MESSAGE)) {
      CSVProcessResponse csvProcessResponse = csvProcessor.processUpdateUserFile(file);
      String errorLogFileName = generateErrorLogFileName(file);
      String modifiedBy = UserContext.getModifiedBy();
      if (csvProcessResponse.getSuccessCount() != 0 && isProcessingSuccessful(csvProcessResponse)) {
        handleUpdatingUsers(csvProcessResponse, response, file, modifiedBy);
      } else if (csvProcessResponse.getSuccessCount() != 0 && csvProcessResponse.getFailureCount()
          != 0 && isPartialProcessing(csvProcessResponse)) {
        handleUpdatingUsersAndLogging(file, action, csvProcessResponse, errorLogFileName,
            response, modifiedBy);
      } else {
        handleErrLogging(file, action, csvProcessResponse, errorLogFileName,
            response);
      }
    }
    return response;
  }

  private void handleUpdatingUsers(CSVProcessResponse csvProcessResponse,
                                   FileUploadResponse response, MultipartFile file,
                                   String modifiedBy) {
    log.info("Updating {} users", csvProcessResponse.getValidUsers().size());
    List<User> updatedCognitoUsers =
        updateUserDataAndUpdateCognitoForUsers(csvProcessResponse.getValidUsers(), modifiedBy);
    if (updatedCognitoUsers.isEmpty()) {
      log.info("No valid user data found to update to the database. Please check the csv"
          + " file and try again.");
      throw new FileProcessingException("No valid user data found to update to the database."
          + " Please check the csv file and try again.");
    }
    log.info("Updated successfully {} users data to the database.",
        updatedCognitoUsers.size());
    response.setSuccessMessage("File " + file.getOriginalFilename()
        + " processed successfully with " + csvProcessResponse.getTotalCount()
        + " records verified and 0 errors.");
  }

  private void handleUpdatingUsersAndLogging(MultipartFile file, String action,
                                             CSVProcessResponse csvProcessResponse,
                                             String errorLogFileName, FileUploadResponse response,
                                             String modifiedBy)
      throws IOException {
    if (csvProcessResponse.getSuccessCount() != 0) {
      log.info("Handling errors while updating {} users", csvProcessResponse.getValidUsers().size());
      List<User> updatedCognitoUsers =
          updateUserDataAndUpdateCognitoForUsers(csvProcessResponse.getValidUsers(), modifiedBy);
      if (updatedCognitoUsers.isEmpty()) {
        log.info("No valid user data found to update to the database. Please check the csv"
            + " file and try again.");
        throw new FileProcessingException("No valid user data found to update to the database."
            + " Please check the csv file and try again.");
      }
      log.info("Updated successfully {} users data to the database.",
          updatedCognitoUsers.size());
    }
    log.info("Error list {}", csvProcessResponse.getErrors().stream().distinct().toList());
    boolean isErrorLogCreated =
        saveErrorLogFileForUploadUsers(csvProcessResponse.getErrors().stream().distinct().toList(),
            errorLogFileName, file.getOriginalFilename(), csvProcessResponse.getFailureCount(),
            csvProcessResponse.getSuccessCount(), csvProcessResponse.getTotalCount(), action);
    if (!isErrorLogCreated) {
      throw new FileStorageException(
          "Failed to save file : " + errorLogFileName);
    }
    response.setErrorLogFileName(errorLogFileName);
    throw new FileProcessingException(
        "File " + file.getOriginalFilename() + " processed partially with "
            + csvProcessResponse.getSuccessCount() + " records verified and "
            + csvProcessResponse.getFailureCount() + " errors.", response);
  }


  private List<User> updateUserDataAndUpdateCognitoForUsers(List<User> validUsers,
                                                            String modifiedBy) {
    List<User> updatedUsers = new ArrayList<>();
    validUsers.forEach(user -> {
      User existingUser =
          userFilterSortDao.getUserByEmailId(user.getEmailId().toLowerCase(),
              Constants.ACTIVE_STATUS);
      if (existingUser != null) {
        existingUser.setFirstName(
            user.getFirstName() == null || user.getFirstName().isEmpty()
                ? existingUser.getFirstName() : user.getFirstName());
        existingUser.setLastName(
            user.getLastName() == null || user.getLastName().isEmpty()
                ? existingUser.getLastName() : user.getLastName());
        existingUser.setInstitutionName(
            user.getInstitutionName() == null || user.getInstitutionName().isEmpty()
                ? existingUser.getInstitutionName() :
                user.getInstitutionName());
        existingUser.setGsiSortFNLN(existingUser.getFirstName() + Constants.HASH
            + existingUser.getLastName());
        existingUser.setName(existingUser.getFirstName().toLowerCase().replace(" ", "")
            + existingUser.getLastName().toLowerCase().replace(" ", ""));
        existingUser.setCountry(
            user.getCountry() == null || user.getCountry().isEmpty()
                ? existingUser.getCountry() : user.getCountry()); //Added country field
        if (user.getUserAccountExpiryDate() != null && !user.getUserAccountExpiryDate().isEmpty()) {
          LocalDate expiryDate = LocalDate.parse(user.getUserAccountExpiryDate(),
              DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT));
          if (expiryDate.isAfter(LocalDate.now())) {
            existingUser.setUserAccountExpiryDate(expiryDate.format(
                DateTimeFormatter.ofPattern(Constants.FIRST_AS_YEAR_FORMAT)));
          }
        }
        if (updateCognitoUser(existingUser)) {
          if (existingUser.getRole().contains(Constants.ROLE_CONTENT_AUTHOR)) {
            updateCourseCreatedByName(existingUser.getEmailId(),
                existingUser.getFirstName(), existingUser.getLastName());
          }
          if (userFilterSortDao.updateUser(existingUser, modifiedBy)) {
            updatedUsers.add(existingUser);
            log.info("User with email {} updated successfully", existingUser.getEmailId());
          } else {
            log.error("Failed to update user with email {}", existingUser.getEmailId());
          }
        } else {
          log.error("Failed to update cognito user with email {}", user.getEmailId());
          throw new RuntimeException(
              "Failed to update cognito user with email " + user.getEmailId());
        }
      } else {
        log.info("User with email {} does not exist or is not active", user.getEmailId());
      }
    });
    return updatedUsers;
  }

  private boolean updateCognitoUser(User user) {
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      log.warn("Cognito user update is not supported in local environment");
      return true;
    } else if (user.getLoginOption().isEmpty() || user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
      try {
        CompletableFuture<Void> future = cognitoService.updateCognitoUserAsync(user);
        future.get();
        return true;
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  public User getUserByPk(String partitionKeyValue) {
    User user;
    user = userFilterSortDao.getUserByPk(partitionKeyValue);
    log.info("Fetching User from userByPk: {}", partitionKeyValue);
    return user;
  }

  @Override
  public String updateUserByPk(String partitionKeyValue, UpdateUserRequest updateUserRequest) {
    User existingUser = userFilterSortDao.getUserByPk(partitionKeyValue);
    if (existingUser == null) {
      throw new UserNotFoundException("User not found with Pk: " + partitionKeyValue);
    }
    validateUpdateUserRequest(updateUserRequest);
    handleStatusUpdate(existingUser, updateUserRequest);
    handleNameChange(existingUser, updateUserRequest);
    updateExistingUserDetails(existingUser, updateUserRequest);
    String modifiedBy = UserContext.getModifiedBy();
    if (userFilterSortDao.updateUserByPk(existingUser, modifiedBy)) {
      return "You have successfully updated User Information details ";
    } else {
      return "User not updated with pk: " + partitionKeyValue;
    }
  }

  private void validateUpdateUserRequest(UpdateUserRequest updateUserRequest) {
      if (updateUserRequest.getFirstName() == null || updateUserRequest.getFirstName().isEmpty()
              || !updateUserRequest.getFirstName().matches(Constants.ALPHABET_REGEX)) {
          throw new ValidationException(
                  "First name must contain only alphabets and cannot be null or empty");
      }
      if (updateUserRequest.getLastName() == null || updateUserRequest.getLastName().isEmpty()
              || !updateUserRequest.getLastName().matches(Constants.ALPHABET_REGEX)) {
          throw new ValidationException(
                  "Last name must contain only alphabets and cannot be null or empty");
      }
      if (updateUserRequest.getInstitutionName() == null
              || updateUserRequest.getInstitutionName().isEmpty()
              || !updateUserRequest.getInstitutionName().matches("[a-zA-Z0-9 ]*")) {
          throw new ValidationException(
                  "Institution name must contain only alphabets, numbers, or spaces and cannot be null or empty");
      }
    validateRole(updateUserRequest.getRole());
    validateUserAccountExpiryDate(updateUserRequest.getUserAccountExpiryDate());
    validateStatus(updateUserRequest.getStatus());
    validateUserType(updateUserRequest.getUserType());
    validateCountry(updateUserRequest.getCountry()); //Validated country field
  }

  private void validateRole(String roleStr) {
    if (roleStr == null || roleStr.isEmpty()) {
      throw new ValidationException("Role is mandatory");
    }
    List<RoleDto> rolesList = roleDao.getRoles();
    List<String> validRoles = rolesList.stream().map(RoleDto::getSk).collect(Collectors.toList());
    List<String> roles = Arrays.asList(roleStr.split(Constants.COMMA_REGEX));
    if (!roles.contains(Constants.ROLE_LEARNER)) {
      throw new ValidationException("cannot remove role 'learner'");
    }
    for (String role : roles) {
      if (!role.equals(Constants.ROLE_LEARNER) && !validRoles.contains(role)) {
        throw new ValidationException("Invalid Role: " + role);
      }
    }
  }

  private void validateUserAccountExpiryDate(String userAccountExpiryDate) {
    if (userAccountExpiryDate == null || userAccountExpiryDate.isEmpty()) {
      throw new ValidationException("User account expiry date cannot be null or empty");
    }
    DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
    try {
      LocalDate.parse(userAccountExpiryDate, dateFormatter);
    } catch (DateTimeParseException e) {
      throw new ValidationException("User account expiry date must be in the format MM/dd/yyyy");
    }
  }

  private void validateStatus(String status) {
    if (status == null || status.isEmpty()
        || (!status.equals(Constants.ACTIVE_STATUS)
        && !status.equals(Constants.IN_ACTIVE_STATUS))) {
      throw new ValidationException(
          "Status should be either 'Active' or 'Inactive' and cannot be null or empty");
    }
  }

  private void validateUserType(String userType) {
    if (userType == null || userType.isEmpty()
        || (!userType.equals(Constants.USER_TYPE_INTERNAL)
        && !userType.equals(Constants.USER_TYPE_EXTERNAL))) {
      throw new ValidationException(
          "User type should be either 'Internal' or 'External' and cannot be null or empty");
    }
  }

  //Validation for country field
  private void validateCountry(String country) {
    if (country == null || country.isEmpty()) {
      throw new ValidationException("Country cannot be null or empty");
    }
    List<String> validCountries = lookupDao.getLookupData("Country", null).stream()
        .map(LookupDto::getName)
        .collect(Collectors.toList());
    if (!validCountries.contains(country.trim())) {
      throw new ValidationException("Invalid Country: " + country + ". Please provide a valid country name");
    }
  }

  private void handleStatusUpdate(User existingUser, UpdateUserRequest updateUserRequest) {
    String status = updateUserRequest.getStatus();
    LocalDate expiryDate = LocalDate.parse(updateUserRequest.getUserAccountExpiryDate(),
        DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT));
    if (status.equals(Constants.IN_ACTIVE_STATUS)) {
      if (!expiryDate.equals(LocalDate.now())) {
        throw new ValidationException(
            "User account expiry date must be the current date when status is 'Inactive'");
      }
      log.info("handleStatusUpdate of user {}", existingUser);
      if (existingUser.getLoginOption() != null && existingUser.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
        var cognitoUserBeforeDisable = cognitoService.getUserFromCognito(existingUser.getEmailId());
        log.info("Cognito user details before disable: {}", (Object) cognitoUserBeforeDisable);
        boolean isSuccess = disableCognitoUser(existingUser);
        var cognitoUserAfterDisable = cognitoService.getUserFromCognito(existingUser.getEmailId());
        log.info("Cognito user details after disable: {}", (Object) cognitoUserAfterDisable);
        if (!isSuccess) {
          throw new RuntimeException(
              "Failed to delete cognito user with email " + existingUser.getEmailId());
        }
      }
      existingUser.setRole(Constants.ROLE_LEARNER);

    } else {
      if (!expiryDate.isAfter(LocalDate.now())) {
        throw new ValidationException(
            "User account expiry date must be at least "
                + "one day after today when status is 'Active'");
      }
      log.info("handleStatusUpdate of user {}", existingUser);

      if (existingUser.getStatus().equalsIgnoreCase(Constants.IN_ACTIVE_STATUS) && updateUserRequest.getStatus().equalsIgnoreCase(Constants.ACTIVE_STATUS)) {
        log.info("reactivating user...");
        handleReactivation(existingUser);
      }
      if (!existingUser.getRole().equalsIgnoreCase(updateUserRequest.getRole())) {
        String previousRole = existingUser.getRole().toLowerCase();
        log.info("Previous role of user {}: {}", existingUser.getEmailId(), previousRole);

        String newRole = updateUserRequest.getRole().toLowerCase();
        log.info("New role of user {}: {}", existingUser.getEmailId(), newRole);

        List<String> rolesToCheck = Constants.TRIGGER_NOTIFICATION_FOR_ADDITIONAL_ROLES;
        List<String> newAssignedRoles = new ArrayList<>();
        rolesToCheck.stream().forEach(role -> {
          if (newRole.contains(role) && !previousRole.contains(role)) {
            newAssignedRoles.add(role);
          }
        });
        if (!newAssignedRoles.isEmpty()) {
          try {
            // Capitalize each word in hyphen-separated role names
            List<String> capitalizedRoles = newAssignedRoles.stream()
                .map(role -> Arrays.stream(role.split("-"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(Collectors.joining(" ")))
                .collect(Collectors.toList());

            String assignedRoles = String.join(", ", capitalizedRoles);
            cognitoService.sendRoleAssignmentEmail(existingUser, assignedRoles);
            log.info("Role change notification sent to user: {}", existingUser.getEmailId());
          } catch (Exception e) {
            log.error("Failed to send role change notification: {}", e.getMessage());

          }
        } else {
          log.info("No new roles assigned to user: {}", existingUser.getEmailId());
        }

      }
    }
  }

  private void handleReactivation(User existingUser) {
    String tempPassword = cognitoService.temporaryPasswordGenerator();
    if (isLmsCredentialsLogin(existingUser)) {
      handleCognitoReactivation(existingUser, tempPassword);
    }
    if (isLmsCredentialsLogin(existingUser) && isPasswordChangeDateGreaterThan90(existingUser)) {
      log.info("sending reactivate email with password");
      handlePasswordResetAndEmail(existingUser, tempPassword);
    } else {
      log.info("sending reactivate email");
      cognitoService.sendReactivateUserEmail(existingUser);
    }
  }

  private boolean handleReactivationcsv(User existingUser) {
    String tempPassword = cognitoService.temporaryPasswordGenerator();
    boolean success = true;

    if (isLmsCredentialsLogin(existingUser)) {
      success = handleCsvCognitoReactivation(existingUser, tempPassword);
    }

    if (isLmsCredentialsLogin(existingUser) && isPasswordChangeDateGreaterThan90(existingUser)) {
      log.info("sending reactivate email with password");
      handlePasswordResetAndEmail(existingUser, tempPassword);
    } else {
      log.info("sending reactivate email");
      cognitoService.sendReactivateUserEmail(existingUser);
    }

    return success;
  }

  private boolean isLmsCredentialsLogin(User existingUser) {
    return existingUser.getLoginOption() == null ||
        existingUser.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS);
  }

  private void handleCognitoReactivation(User existingUser, String tempPassword) {
    AdminGetUserResponse response = cognitoService.getUserFromCognito(existingUser.getEmailId());
    log.info("cognito user response {}", response);
    if (response != null) {
      log.info("User exists in Cognito {} ", existingUser.getEmailId());

      if (!response.enabled()) {
        log.info("User is disabled in Cognito. Enabling now...");
        boolean isSuccess = enableCognitoUser(existingUser);

        if (!isSuccess) {
          throw new RuntimeException(
              "Failed to enable cognito user with email " + existingUser.getEmailId());
        }
      } else {
        log.info("User is already enabled");
      }
    } else {
      cognitoService.recreateCognitoUserAsync(existingUser, tempPassword);
      log.info("handleStatusUpdate of new user created as user was not present {}", existingUser);
    }
  }

  private boolean handleCsvCognitoReactivation(User existingUser, String tempPassword) {
    try {
      AdminGetUserResponse response = cognitoService.getUserFromCognito(existingUser.getEmailId());
      log.info("Cognito user response: {}", response);

      if (response != null) {
        log.info("User exists in Cognito: {}", existingUser.getEmailId());

        if (!response.enabled()) {
          log.info("User is disabled in Cognito. Enabling now...");
          boolean isSuccess = enableCognitoUser(existingUser);
          if (!isSuccess) {
            log.error("Failed to enable Cognito user with email: {}", existingUser.getEmailId());
            return false;
          }
        } else {
          log.info("User is already enabled");
          return true;
        }
      } else {
        try {
          cognitoService.recreateCognitoUserAsync(existingUser, tempPassword);
          log.info("New user created as user was not present: {}", existingUser);
          return true;
        } catch (Exception e) {
          log.error("Failed to create Cognito user for email: {}. Error: {}", existingUser.getEmailId(), e.getMessage(), e);
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      log.error("Unexpected error during Cognito reactivation for user: {}. Error: {}", existingUser.getEmailId(), e.getMessage(), e);
      return false;
    }
  }

  private void handlePasswordResetAndEmail(User existingUser, String tempPassword) {
    log.info("password reset email for reactivation....");

    try {
      CompletableFuture<Void> future = cognitoService.setCognitoUserPasswordAsync(
          existingUser.getEmailId(), tempPassword, existingUser);
      future.get();

      if (future.isDone() && !future.isCompletedExceptionally()) {
        cognitoService.sendReactivateSkillSpringUserEmail(existingUser, tempPassword);
        log.info("Temporary password email sent successfully to user '{}'.", existingUser.getEmailId());
        updatePasswordChangedDate(existingUser);
      } else {
        log.error("Failed to set temporary password for user '{}'.", existingUser.getEmailId());
      }
    } catch (Exception ex) {
      log.error("Error setting temporary password for user '{}': {}", existingUser.getEmailId(), ex.getMessage(), ex);
    }
  }

  private boolean enableCognitoUser(User existingUser) {
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      log.warn("Cognito user disable is not supported in local environment");
      return true;
    } else if (existingUser.getLoginOption().isEmpty() || existingUser.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
      try {
        CompletableFuture<Void> future = cognitoService.enableCognitoUserAsyncWithEMail(existingUser.getEmailId());
        future.get();
        return true;
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    } else {
      log.warn("Cognito user disable is not supported for this login option");
      throw new RuntimeException("Cognito user disable is not supported for this login option");
    }
  }


  private boolean disableCognitoUser(User existingUser) {
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      log.warn("Cognito user disable is not supported in local environment");
      return true;
    } else if (existingUser.getLoginOption().isEmpty() || existingUser.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
      try {
        CompletableFuture<Void> future = cognitoService.disableCognitoUserAsync(existingUser);
        future.get();
        return true;
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    } else {
      log.warn("Cognito user disable is not supported for this login option");
      throw new RuntimeException("Cognito user disable is not supported for this login option");
    }
  }

  private void handleNameChange(User existingUser, UpdateUserRequest updateUserRequest) {
    boolean isFirstNameChanged = existingUser.getFirstName() != null
        && !existingUser.getFirstName().equals(updateUserRequest.getFirstName());
    boolean isLastNameChanged = existingUser.getLastName() != null
        && !existingUser.getLastName().equals(updateUserRequest.getLastName());
    if (isFirstNameChanged || isLastNameChanged) {
      User updateUser = new User();
      updateUser.setFirstName(updateUserRequest.getFirstName());
      updateUser.setLastName(updateUserRequest.getLastName());
      updateUser.setEmailId(existingUser.getEmailId());
      updateUser.setLoginOption(existingUser.getLoginOption() != null ?
          existingUser.getLoginOption() : Constants.EMPTY_STRING);
      if (existingUser.getRole().contains(Constants.ROLE_CONTENT_AUTHOR)) {
        updateCourseCreatedByName(existingUser.getEmailId(),
            updateUserRequest.getFirstName(), updateUserRequest.getLastName());
      }
      boolean isSuccess = updateCognitoUser(updateUser);
      if (!isSuccess) {
        throw new RuntimeException(
            "Failed to update cognito user with email " + existingUser.getEmailId());
      }
    }
  }

  private void updateCourseCreatedByName(String emailId, String firstName, String lastName) {
    String fullName = firstName + " " + lastName;
    try {
      log.info("Updating course created by name for emailId: {}", emailId);
      String response = courseManagementServiceClient.updateCourseCreatedByName(emailId, fullName);
      if (response == null) {
        log.error("Failed to update course created by name for emailId: {}", emailId);
        throw new RuntimeException("Failed to update course created by name for emailId: " + emailId);
      } else {
        log.info("Course created by name updated successfully for emailId: {}", emailId);
      }
    } catch (Exception e) {
      log.error("Failed to update course created by name: {}", e.getMessage());
    }
  }

  private void updateExistingUserDetails(User existingUser, UpdateUserRequest updateUserRequest) {
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
    String updatedOn = utcDateTime.format(formatter);
    existingUser.setFirstName(updateUserRequest.getFirstName()
        .substring(0, 1).toUpperCase()
        + updateUserRequest.getFirstName().substring(1));
    existingUser.setLastName(updateUserRequest.getLastName().substring(0, 1).toUpperCase()
        + updateUserRequest.getLastName().substring(1));
    existingUser.setInstitutionName(updateUserRequest.getInstitutionName());
    String[] expiryDateStr = updateUserRequest.getUserAccountExpiryDate().split("/");
    String formattedExpiryDate = expiryDateStr[2] + "/" + expiryDateStr[0] + "/" + expiryDateStr[1];
    existingUser.setUserAccountExpiryDate(formattedExpiryDate);
    existingUser.setUserType(updateUserRequest.getUserType());
    existingUser.setRole(updateUserRequest.getRole());
    DateTimeFormatter formatterForLastLoginTimeStamp = DateTimeFormatter.ofPattern(Constants.TIMESTAMP);
    String reActivatedDate = utcDateTime.format(formatterForLastLoginTimeStamp);
    if (!existingUser.getStatus().equalsIgnoreCase(updateUserRequest.getStatus())) {
      existingUser.setReactivatedDate(reActivatedDate);
    }
    existingUser.setStatus(updateUserRequest.getStatus());
    existingUser.setCountry(updateUserRequest.getCountry().trim());
    existingUser.setModifiedOn(updatedOn);
    existingUser.setModifiedBy(UserContext.getModifiedBy());
    existingUser.setGsiSortFNLN(
        existingUser.getFirstName() + Constants.HASH + existingUser.getLastName());
    existingUser.setName(existingUser.getFirstName().toLowerCase().replace(" ", "")
        + existingUser.getLastName().toLowerCase().replace(" ", ""));
  }

  private void persistLogFileData(String action, String fileName) {
    try {
      OperationsHistory logFileData = new OperationsHistory();
      logFileData.setPk(
          TenantUtil.getTenantCode() + Constants.HASH + Constants.AREA_USER_MANAGEMENT);
      logFileData.setSk(ZonedDateTime.now()
          .format(DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP))
          + Constants.HASH + action);
      logFileData.setFileName(fileName);
      logFileData.setCreatedOn(ZonedDateTime.now()
          .format(DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP)));
      logFileData.setUploadedBy(UserContext.getCreatedBy());
      logFileData.setEmail(UserContext.getUserEmail());
      logFileData.setTenantCode(TenantUtil.getTenantCode());
      logFileData.setOperation(action);
      logFileData.setArea(Constants.AREA_USER_MANAGEMENT);
      log.info("Generating log file data for id: {}", logFileData.getPk());
      operationsHistoryDao.saveLogFileData(logFileData);
    } catch (Exception e) {
      log.error("Failed to persist log file data: {}", e.getMessage());
      throw new RuntimeException("Failed to persist log file data: " + e.getMessage());
    }
  }

  @Override
  public String sendWelcomeEmail(String email) throws Exception {
    try {
      User existingUser = getUserByEmail(email);
      return sendWelcomeEmailToUser(existingUser);
    } catch (CognitoIdentityProviderException e) {
      throw new Exception("Error checking email in Cognito: " + e.getMessage(), e);
    }
  }

  private User getUserByEmail(String email) {
    User existingUser = userFilterSortDao.getUserByEmailId(email, Constants.ACTIVE_STATUS);
    if (existingUser == null) {
      throw new UserNotFoundException("User not found in database with email: " + email);
    }
    return existingUser;
  }

  private String sendWelcomeEmailToUser(User existingUser) throws Exception {
    return cognitoService.sendWelcomeEmail(existingUser);
  }

  @Override
  public User getUserByEmailId(String emailId, String status) {

    User existingUser = userFilterSortDao.getUserByEmailId(emailId, status);
    if (existingUser == null) {
      throw new UserNotFoundException("User not found in database with email: " + emailId);
    }
    return existingUser;
  }

  @Override
  public String scheduleUserExpiry() {
    try {
      log.info("Fetching expired users whose date is expired and status is Active");
      Set<String> emails = userFilterSortDao.getExpiredUsers();
      if (emails.size() == 0) {
        log.info("No expired user found");
        return Constants.NO_EXPIRED_USER_FOUND;
      }
      log.info("Expired user emails: {}", emails.size());
      List<String> deactivatedEmails = deactivateUserDataAndDeleteCognitoForUsers(emails,
          Constants.SYSTEM);
      log.info("Deactivated users emails: {}", deactivatedEmails.size());
      return Constants.EXPIRED_USER_DEACTIVATED;
    } catch (Exception e) {
      log.error("Failed to deactivate users: {}", e.getMessage());
      throw new RuntimeException("Failed to deactivate users: " + e.getMessage());
    }
  }

  @Override
  public LoggedInUser registerLoggedInUser(String deviceDetails, String ipAddress, String portal) {
    log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
        ProcessConstants.IN_PROGRESS) + "registerLoggedInUser Fetching logged in user");
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AuthUser authUser = (AuthUser) auth.getPrincipal();

    LoggedInUser loggedInUser = new LoggedInUser();
    BeanUtils.copyProperties(authUser, loggedInUser, "token");
    loggedInUser.setName(authUser.getUsername());
    User user = getUserByEmail(authUser.getUserEmail());
    loggedInUser.setFirstName(user.getFirstName());
    loggedInUser.setLastName(user.getLastName());
    loggedInUser.setUserId(user.getPk());
    loggedInUser.setTermsAccepted(user.getTermsAccepted());
    loggedInUser.setTermsAcceptedDate(user.getTermsAcceptedDate());
    loggedInUser.setUserType(user.getUserType());
    loggedInUser.setLoginOption(user.getLoginOption());
    loggedInUser.setPortal(user.getPortal());
    loggedInUser.setPreferredUI(user.getPreferredUI() != null ? user.getPreferredUI() : Constants.PREFERRED_UI_CLASSIC);
    loggedInUser.setIsWatchedTutorial(user.getIsWatchedTutorial());
    loggedInUser.setVideoLaunchCount(user.getVideoLaunchCount());
    loggedInUser.setTutorialWatchDate(user.getTutorialWatchDate());
    loggedInUser.setCountry(user.getCountry());

    String tenantCode = TenantUtil.getTenantCode();
    TenantConfigDto tenantConfig = teanatTableDao.fetchTenantConfig(tenantCode);
    String rolesString = String.join(",", loggedInUser.getUserRoles());

    TermsAndUseDTO termsAndUse = teanatTableDao.getTermsAndUse(tenantCode);
    loggedInUser.setTermsAndUse(termsAndUse);

    if (tenantConfig.getCookieDomain().equals(rootDomainPath) && rolesString.contains("super-admin")) {
      tenantConfig.setSuperAdmin("true");
      tenantConfig.setTenantAdmin("false");
    } else {
      if (rolesString.contains("system-admin") || rolesString.contains("super-admin")) {
        tenantConfig.setTenantAdmin("true");
        tenantConfig.setSuperAdmin("false");
      }
    }
    loggedInUser.setTenantConfig(tenantConfig);

    String userName = user.getFirstName() + " " + user.getLastName();

    userFilterSortDao.updateFirstLoggedInUser(user, userName);
    loggedInUser.setTenantCode(user.getTenantCode());

    log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
        ProcessConstants.COMPLETED) + "registerLoggedInUser Fetching logged in user");

//    if (portal != null) {
//      if (!portal.contains(loggedInUser.getPortal())) {
//        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not authorized for this portal");
//      }
//    }

    try {
      String tokenIssuedAt = TokenUtil.extractIssuedAtTimeStamp(authUser.getToken());
      if(!deviceDetails.isEmpty() && !ipAddress.isEmpty() && isFreshLogin(user.getPk(), tokenIssuedAt)) {
        userActivityLogService.saveUserActivityLog(deviceDetails, ipAddress, Constants.USER_ACTIVITY_LOGIN_TYPE);
      }
    } catch (Exception e) {
        log.error("Error saving login user activity: {}", e.getMessage());
    }
    return loggedInUser;
  }

  @Override
  public List<Map<String, String>> validUsers(List<String> userEmails) {
    return userEmails.stream()
        .map(email -> userFilterSortDao.getUserByEmailId(email, Constants.ACTIVE_STATUS))
        .filter(Objects::nonNull)
        .map(user -> Map.of("userId", user.getPk(), "email", user.getEmailId(), "name", user.getFirstName() + " " + user.getLastName()))
        .toList();
  }

  @Override
  public boolean recreateUserInCognitoAndSendWelcomeEmail(String email) {
    try {
      User user = userFilterSortDao.getUserByEmailId(email, Constants.ACTIVE_STATUS);
      if (user == null) {
        log.error("User not found in database");
        return false;
      }
      boolean isDeleted = deleteCognitoUser(user);
      if (!isDeleted) {
        log.error("Failed to delete user from Cognito");
        return false;
      }
      log.info("Successfully sent welcome email to user");
      return true;
    } catch (Exception e) {
      log.error("Error occurred while recreating user in Cognito and sending welcome email");
      return false;
    }
  }


  @Override
  public void migratePasswordChangedDate() {
    log.info("Starting migration to add passwordChangedDate attribute to all users.");
    List<User> users;
    try {
      users = userDao.getAllUsers();
    } catch (Exception e) {
      log.error("Failed to fetch users from the database: {}", e.getMessage(), e);
      return;
    }

    for (User user : users) {
      try {
        if (user.getPasswordChangedDate() == null && user.getLastLoginTimestamp() != null) {
          user.setPasswordChangedDate(user.getLastLoginTimestamp());
          userDao.addPasswordChangedDate(user.getPk(), user.getSk(), user.getEmailId(), user.getPasswordChangedDate());
          log.info("Updated user {} with passwordChangedDate: {}", user.getEmailId(), user.getPasswordChangedDate());
        }
      } catch (Exception e) {
        log.error("Failed to update user {}: {}", user.getEmailId(), e.getMessage(), e);
      }
    }

    log.info("Migration completed successfully.");
  }

  @Override
  public String updateLastLoginTimeStampAndPasswordChangedDate(UpdateDateDTO updateDateDTO) {
    User user = userFilterSortDao.getUserByEmailId(updateDateDTO.getEmailId(), Constants.ACTIVE_STATUS);
    if (user == null) {
      throw new UserNotFoundException("User not found with email: " + updateDateDTO.getEmailId());
    }
    userDao.updateLastLoginTimeStampAndPasswordChangedDate(user.getPk(), user.getSk(), updateDateDTO);
    return "User date fields updated successfully.";
  }

  @Override
  public boolean setPasswordAndSendEmailAsync(String emailId) {
    try {
      User user = userFilterSortDao.getUserByEmailId(emailId, Constants.ACTIVE_STATUS);
      if (user == null) {
        log.error("User with email ID '{}' not found in the database.", emailId);
        return false;
      }
      String tempPassword;
      try {
        tempPassword = cognitoService.temporaryPasswordGenerator();
        log.info("Temporary password generated successfully for user '{}'.", emailId);
      } catch (Exception e) {
        log.error("Failed to generate a temporary password for user '{}'. Error: {}", emailId, e.getMessage());
        throw new RuntimeException("Error generating temporary password", e);
      }

      try {
        CompletableFuture<Void> future = cognitoService.setCognitoUserPasswordAsync(user.getEmailId(), tempPassword, user);
        future.get();

        if (future.isDone() && !future.isCompletedExceptionally()) {
          cognitoService.sendTemporaryPasswordEmail(user, tempPassword);
          log.info("Temporary password email sent successfully to user '{}'.", emailId);

          updatePasswordChangedDate(user);
          log.info("PasswordChangedDate updated for user '{}'.", emailId);
        } else {
          log.error("Failed to set temporary password for user '{}'. Operation did not complete successfully.", emailId);
          return false;
        }
      } catch (Exception ex) {
        log.error("An error occurred while setting the temporary password or sending the email for user '{}': {}", emailId, ex.getMessage(), ex);
        return false;
      }

      log.info("Temporary password set and email sent successfully for user '{}'.", emailId);
      return true;
    } catch (Exception e) {
      log.error("An unexpected error occurred while processing the request for user '{}': {}", emailId, e.getMessage(), e);
      return false;
    }
  }

  @Override
  public boolean enableCognitoUserByEmail(String email) throws Exception {
    var cognitoUser = cognitoService.getUserFromCognito(email);
    log.info("Cognito user details before enable: {}", (Object) cognitoUser);
    cognitoService.enableCognitoUserAsyncWithEMail(email).join();
    // Fetch the data after login
    var cognitoUserAfter = cognitoService.getUserFromCognito(email);
    log.info("Cognito user details after enable: {}", (Object) cognitoUserAfter);
    return true;
  }

  @Override
  public boolean disableCognitoUserByEmail(String email) throws Exception {
    var cognitoUser = cognitoService.getUserFromCognito(email);
    log.info("Cognito user details before disable: {}", (Object) cognitoUser);
    cognitoService.disableCognitoUserAsyncWithEmail(email).join();
    var cognitoUserAfter = cognitoService.getUserFromCognito(email);
    log.info("Cognito user details after disable: {}", (Object) cognitoUserAfter);
    return true;
  }

  public void updatePasswordChangedDate(User user) {

    ZonedDateTime currentDateTime = ZonedDateTime.now();
    String formattedDateTime = currentDateTime.format(DateTimeFormatter.ISO_INSTANT);

    user.setPasswordChangedDate(formattedDateTime);

    userDao.addPasswordChangedDate(user.getPk(), user.getSk(), user.getEmailId(), formattedDateTime);

    log.info("PasswordChanged Date updated to current date for user: {}", user.getEmailId());
  }

  private boolean isPasswordChangeDateGreaterThan90(User user) {
    if (user.getPasswordChangedDate() == null || user.getPasswordChangedDate().isEmpty()) {
      log.error("User with email {} does not have a valid password changed date", user.getEmailId());
      return false;
    }

    ZonedDateTime passwordChangedDate = ZonedDateTime.parse(user.getPasswordChangedDate(), DateTimeFormatter.ofPattern(Constants.TIMESTAMP));
    if (passwordChangedDate.plusDays(90).isBefore(ZonedDateTime.now())) {
      return true;
    }
    log.error("User with email {} has a password changed date older than 90 days", user.getEmailId());
    return false;
  }

  @Override
  public void updateTermsAccepted() {
    if (userFilterSortDao.updateTermsAccepted()) {
      log.info("Terms accepted updated successfully ");
    } else {
      log.error("Failed to update terms accepted ");
      throw new RuntimeException("Failed to update terms accepted for user: ");
    }
  }

  @Override
  public void updateTermsAcceptedByPartitionKeyValue(String partitionKeyValue) {
    User user = userFilterSortDao.getUserByPk(partitionKeyValue);
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
    String formattedDate = utcDateTime.format(formatter);
    if (user == null) {
      log.error("User not found with partitionKey: {}", partitionKeyValue);
      throw new UserNotFoundException("User not found with partitionKey: " + partitionKeyValue);
    }
    if (userFilterSortDao.updateUserTermsAccepted(user.getPk(), user.getSk(), "Y", formattedDate)) {
      log.info("Terms accepted updated successfully for user: {}", user.getName());
    } else {
      log.error("Failed to update terms accepted for user: {}", user.getName());
      throw new RuntimeException("Failed to update terms accepted for user: " + user.getName());
    }
  }

  @Override
  public FileUploadResponse uploadBulkUsers(MultipartFile file, String action) throws Exception {
    log.info("Uploading bulk users file: {}", file.getOriginalFilename());
    fileUtil.validateFile(file);
    String fileS3Key = generateFileS3Key(file.getOriginalFilename());
    String fileResponse = uploadBulkUsersFileWithTimestamp(file, fileS3Key);
    log.info(fileResponse);

    //pk and sk of log table
    String createdOn = ZonedDateTime.now()
        .format(DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP));
    String pk = TenantUtil.getTenantCode() + Constants.HASH + Constants.AREA_USER_MANAGEMENT;
    String sk = createdOn + Constants.HASH + action;
    AddUserEventDetailDto addUserEventDetailDto = new AddUserEventDetailDto();
    addUserEventDetailDto.setEventType(Constants.ADD_USER_EVENT);
    addUserEventDetailDto.setFileName(fileS3Key);
    addUserEventDetailDto.setAction(action);
    addUserEventDetailDto.setCreatedBy(UserContext.getCreatedBy());
    addUserEventDetailDto.setTenantCode(TenantUtil.getTenantCode());
    addUserEventDetailDto.setIdpPreferences(TenantUtil.getTenantDetails().getIdpPreferences());
    addUserEventDetailDto.setUserRoles(UserContext.getUserRoles());
    addUserEventDetailDto.setPk(pk);
    addUserEventDetailDto.setSk(sk);
    FileUploadResponse fileUploadResponse = new FileUploadResponse();
    log.info("Reading and processing CSV file: {}", file.getOriginalFilename());
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
        StandardCharsets.ISO_8859_1))) {
      CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader);
      int totalRecords = csvParser.getRecords().size();
      log.info("Total records in CSV: {}", csvParser.getRecords().size());
      addUserEventDetailDto.setRowNum(totalRecords);
      boolean validationHeaders =
          csvValidator.validateHeaders(csvParser.getHeaderMap(), Constants.VALID_ADD_USERS_HEADERS);
      log.info(validationHeaders ? "CSV headers validation successful."
          : "CSV header validation failed.");
      if (validationHeaders) {
        log.info("CSV headers validation successful.");
        createLogUserUploadLog(action, fileS3Key, pk, sk, Constants.STATUS_IN_PROGRESS, Constants.ZERO_PERCENT, null);
        log.info("Triggering event to process bulk user addition.");
        userManagementEventPublisherService.triggerAddUserPublishEvent(addUserEventDetailDto);
      } else {
        List<String> errors = new ArrayList<>();
        errors.add(String.format(
            "%s--%s",
            LocalDateTime.now().format(DateTimeFormatter
                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
            "Invalid Header: Must be one of " + Constants.VALID_ADD_USERS_HEADERS));
        String errorLogFileName = generateErrorLogFileName(file);
        boolean isSaved = saveErrorLogFileForUploadBulkUsers(errors, errorLogFileName, fileS3Key, totalRecords, 0, totalRecords, action);
        if (isSaved) {
          createLogUserUploadLog(action, fileS3Key, pk, sk, Constants.FAILED, Constants.HUNDRED_PERCENT, errorLogFileName);
        } else {
          log.info("Failed to save error log file for bulk user upload (Invalid header) : {}", errorLogFileName);
          throw new RuntimeException("Failed to save error log file for bulk user upload (Invalid header) : " + errorLogFileName);
        }
        log.info("CSV header validation failed.");
        fileUploadResponse.setErrorLogFileName(errorLogFileName);
        throw new FileProcessingException("File " + file.getOriginalFilename() + " failed with 0 errors.", fileUploadResponse);
      }

    } catch (IOException e) {
      log.error("Error processing CSV file: {}", e.getMessage());
      throw new RuntimeException("Error processing CSV file", e);
    }
    fileUploadResponse.setSuccessMessage("File uploaded successfully");
    return fileUploadResponse;
  }

  @Override
  public String deleteBulkUsers(MultipartFile file, int start, int end) {
    List<User> users = csvProcessor.processDeleteBulkUsers(file, start, end);
    users.forEach(user -> {
      try {
        cognitoService.deleteBulkCognitoUserAsync(user).join();
      } catch (Exception e) {
        log.info("Failed to delete user from Cognito: {}", e.getMessage());
      }
    });
    userFilterSortDao.deleteBatchUsers(users);
    return "Deleted users successfully";
  }

  @Override
  public void updateUserSettings(String userId, String type, String option) {
  try {
    userFilterSortDao.updateUserSettings(userId, type, option);
    } catch (Exception e) {
      log.error("Failed to update user settings for userId: {}: {}", userId, e.getMessage());
      throw new RuntimeException("Failed to update user settings for userId: " + userId, e);
    }   log.info("Updated user settings for userId: {}", userId);
  }

  private String uploadBulkUsersFileWithTimestamp(MultipartFile file, String fileName) throws Exception {
    log.info("Uploading file {} for bulk user creation with timestamp", file.getOriginalFilename());
    fileUtil.validateFile(file);
    boolean isFileSaved = false;
    String storageLocationMsg = "";
    if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
      isFileSaved =
          fileUtil.saveFileToLocal(file, localStoragePath, Constants.LOCAL_DISK_PREFIX_USERDATA);
      storageLocationMsg = Constants.storedInLocalMsg;
    } else {
      isFileSaved = s3Utils.saveFileToS3(file, bucketName,
          TenantUtil.getTenantCode() + Constants.S3_PREFIX + fileName);
      storageLocationMsg = Constants.storedInS3Msg;
    }
    if (!isFileSaved) {
      throw new FileStorageException("Failed to save file" + file.getOriginalFilename());
    }
    return storageLocationMsg;
  }

  private String generateFileS3Key(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("File name cannot be null");
    }
    int dotIndex = fileName.lastIndexOf('.');
    String timestamp = LocalDateTime.now().format(DateTimeFormatter
        .ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT));
    return dotIndex == -1 ? fileName + "_" + timestamp
        : fileName.substring(0, dotIndex) + "_" + timestamp + fileName.substring(dotIndex);
  }


  private void createLogUserUploadLog(String action, String fileName, String pk,
                                      String sk, String status, String progress, String errorFileName) {
    try {
      OperationsHistory logFileData = new OperationsHistory();
      logFileData.setPk(pk);
      logFileData.setSk(sk);
      if (errorFileName != null) {
        logFileData.setFileName(errorFileName);
      }
      logFileData.setCreatedOn(ZonedDateTime.now()
          .format(DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP)));
      logFileData.setUploadedBy(UserContext.getCreatedBy());
      logFileData.setEmail(UserContext.getUserEmail());
      logFileData.setTenantCode(TenantUtil.getTenantCode());
      logFileData.setOperation(action);
      logFileData.setArea(Constants.AREA_USER_MANAGEMENT);
      logFileData.setUploadedFileS3key(fileName);
      logFileData.setFileStatus(status);
      logFileData.setFileProgress(progress);
      log.info("Generating log file data for id: {}", logFileData.getPk());
      operationsHistoryDao.saveLogFileData(logFileData);
    } catch (Exception e) {
      log.error("Failed to persist log file data: {}", e.getMessage());
      throw new RuntimeException("Failed to persist log file data: " + e.getMessage());
    }
  }

  @Override
  public Map<String, String> getUserByUsername(List<String> usernames) {
    return userFilterSortDao.getUserEmailsByUsernames(usernames);
  }

  private boolean isFreshLogin(String pk, String tokenIssuedAt) {
    return userActivityLogDao.findByTimestamp(pk, tokenIssuedAt).isEmpty();
  }

  @Override
  public String updatePreferredUI(String userId, String preferredUI) {
    User existingUser = userFilterSortDao.getUserByPk(userId);
    if (existingUser == null) {
      throw new UserNotFoundException("User not found with Pk: " + userId);
    }
    userFilterSortDao.updateLearnerPreferredView(existingUser, preferredUI);
    log.info("Preferred UI updated successfully to {} for user: {}", preferredUI, userId);
    return "Preferred UI updated successfully to " + preferredUI;
  }

  @Override
  public List<UserEmailDto> listUserEmailIdAndUserId() {
    return userFilterSortDao.listUserEmailIdAndUserId();
  }

  @Override
  public void updateIsWatchedTutorial(String userId) {
    User user = userFilterSortDao.getUserByPk(userId);
    if (user == null) {
      log.error("User not found with partitionKey: {}", userId);
      throw new UserNotFoundException("User not found with partitionKey: " + userId);
    }
    if (userFilterSortDao.updateIsWatchedTutorial(user.getPk(), user.getSk())) {
      log.info("video tutorial is watched successfully for user: {}", user.getName());
    }
  }

  @Override
  public void recordVideoLaunch(String userId) {
    User user = userFilterSortDao.getUserByPk(userId);
    if (user == null) {
      log.error("User not found with partitionKey: {}", userId);
      throw new UserNotFoundException("User not found with partitionKey: " + userId);
    }
    if (userFilterSortDao.updateVideoLaunchCount(user.getPk(), user.getSk())) {
      log.info("video tutorial is watched successfully for user: {}", user.getName());
    }
  }
    @Override
    public List<User> getUserByTenantCode(String partitionKeyValue) {
        log.info("Fetching users for tenant code: {}", partitionKeyValue);
        return userFilterSortDao.getUserByTenantCode(partitionKeyValue);
    }

    @Override
    public Map<String, Integer> getUserCountByTenantCodes(List<String> tenantCodes) {
        if (tenantCodes == null || tenantCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String tenantCode : tenantCodes) {
            if (tenantCode == null || tenantCode.trim().isEmpty()) {
                continue;
            }
            List<User> users = getUserByTenantCode(tenantCode.trim());
            counts.put(tenantCode.trim(), users == null ? 0 : users.size());
        }
        return counts;
    }
  @Override
  public boolean addAdminUser(User user, String role, String tenantCode) {
        log.info("Adding admin user with email: {} to tenant: {}", user.getEmailId(), tenantCode);
        TenantConfigDto tenantConfig = teanatTableDao.fetchTenantConfig(tenantCode);
        if (tenantConfig == null) {
            throw new IllegalArgumentException("Invalid tenant tenantCode: " + tenantCode);
        }
        User existingUser = userFilterSortDao.getUserByEmailIdAndTenant(user.getEmailId(), Constants.ACTIVE_STATUS,tenantCode);
        if (existingUser != null) {
            throw new ValidationException("EmailId already exists");
        }

        user.setTenantCode(tenantCode);
        String institutionName = "t-2".equals(user.getTenantCode()) ? "Cognizant" : user.getTenantCode();
        user.setInstitutionName(institutionName);
        if ("superadmin".equalsIgnoreCase(role)) {
            user.setRole(Constants.SUPER_ADMIN_ROLE); // e.g., "super-admin"
        } else if ("tenantadmin".equalsIgnoreCase(role)) {
            user.setRole(Constants.SYSTEM_ADMIN_ROLE); // e.g., "system-admin"
        } else {
            user.setRole(role);
        }
        user.setUserType("Internal");
        ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        String createdOn = utcDateTime.format(formatter);
        user.setCreatedOn(createdOn);
        try {
            CompletableFuture<Void> future = cognitoService.createUserWithPoolIdAsync(user, tenantConfig.getUserPoolId());
            future.get(); // Wait for completion
            createUser(user);  // adding to db
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            log.error("Failed to create Cognito user: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error:{}", e.getMessage());
            return false;
        }
  }

  @Override
  public void updateUserPersonalDetails(String pk, String sk, String firstName, String lastName, String country, String institutionName, String currentRole) {
    try {
      log.info("Updating personal details for user with pk: {}", pk);
      userDao.updateUserPersonalDetails(pk, sk, firstName, lastName, country, institutionName, currentRole);
      log.info("Successfully updated personal details for user with pk: {}", pk);
    } catch (Exception e) {
      log.error("Error updating user personal details: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to update user personal details", e);
    }
  }

  @Override
  public String uploadProfilePhoto(String pk, String sk, MultipartFile file) throws Exception {
    try {
      log.info("Uploading profile photo for user with pk: {} and sk: {}", pk, sk);
      
      // Generate unique filename with timestamp
      String timestamp = Instant.now().toEpochMilli() + "";
      String originalFilename = file.getOriginalFilename();
      String fileExtension = originalFilename != null && originalFilename.contains(".") 
          ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
          : ".jpg";
      String fileName = "profile-photo-" + timestamp + fileExtension;
      
      // Define S3 path: profile-photos/{pk}/
      String s3Key = "profile-photos/" + pk + "/" + fileName;
      
      log.info("Uploading file to S3 with key: {}", s3Key);
      
      // Upload to S3
      s3Utils.uploadFileToS3(bucketName, s3Key, file.getBytes(), file.getContentType());
      
      // Construct S3 URL
      String photoUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
      
      // Update user record with photo URL
      log.info("Updating user record with profile photo URL");
      userDao.updateProfilePhotoUrl(pk, sk, photoUrl);
      
      log.info("Successfully uploaded profile photo for user with pk: {}", pk);
      return photoUrl;
    } catch (Exception e) {
      log.error("Error uploading profile photo for user with pk: {} and sk: {}: {}", pk, sk, e.getMessage(), e);
      throw new FileStorageException("Failed to upload profile photo: " + e.getMessage());
    }
  }

  @Override
  public void deleteProfilePhoto(String pk, String sk, String photoUrl) throws Exception {
    try {
      log.info("Deleting profile photo for user with pk: {}", pk);

      // Extract S3 key from URL
      // URL format: https://bucket-name.s3.amazonaws.com/profile-photos/USER#email/profile-photo-timestamp.jpg
      String s3Key = extractS3KeyFromUrl(photoUrl);
      
      if (s3Key != null && !s3Key.isEmpty()) {
        // Delete from S3
        s3Utils.deleteFileFromS3(bucketName, s3Key);
        log.info("Deleted file from S3: {}", s3Key);
      }

      // Update DynamoDB to clear the profilePhotoUrl field
      userDao.updateProfilePhotoUrl(pk, sk, null);
      
      log.info("Successfully deleted profile photo for user with pk: {}", pk);
    } catch (Exception e) {
      log.error("Error deleting profile photo for user with pk: {} and sk: {}: {}", pk, sk, e.getMessage(), e);
      throw new FileStorageException("Failed to delete profile photo: " + e.getMessage());
    }
  }

  private String extractS3KeyFromUrl(String photoUrl) {
    try {
      // Extract key from URL like: https://bucket.s3.amazonaws.com/profile-photos/USER#email/file.jpg
      // Result should be: profile-photos/USER#email/file.jpg
      if (photoUrl.contains(".s3.amazonaws.com/")) {
        String[] parts = photoUrl.split("\\.s3\\.amazonaws\\.com/");
        if (parts.length > 1) {
          return parts[1].split("\\?")[0]; // Remove query params if any
        }
      }
      return null;
    } catch (Exception e) {
      log.error("Error extracting S3 key from URL: {}", photoUrl, e);
      return null;
    }
  }

  @Override
  public String getPresignedProfilePhotoUrl(String photoUrl) throws Exception {
    try {
      if (photoUrl == null || photoUrl.trim().isEmpty()) {
        return null;
      }

      // Extract S3 key from the stored URL
      String s3Key = extractS3KeyFromUrl(photoUrl);
      
      if (s3Key == null || s3Key.isEmpty()) {
        log.error("Could not extract S3 key from URL: {}", photoUrl);
        return photoUrl; // Return original URL as fallback
      }

      // Generate presigned URL valid for 60 minutes
      return s3Utils.generatePresignedUrl(bucketName, s3Key, 60);
    } catch (Exception e) {
      log.error("Error generating presigned URL: {}", e.getMessage(), e);
      return photoUrl; // Return original URL as fallback
    }
  }

  @Override
  public void updateModalShownStatus(String pk, String sk, String modalType, boolean shown) {
    try {
      log.info("Updating modal shown status for user with pk: {}, modalType: {}, shown: {}", pk, modalType, shown);
      userDao.updateModalShownStatus(pk, sk, modalType, shown);
      log.info("Successfully updated modal shown status for user with pk: {}", pk);
    } catch (Exception e) {
      log.error("Error updating modal shown status: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to update modal shown status", e);
    }
  }

  @Override
  public void updateTermsAccepted(String pk, String sk) {
    try {
      log.info("Updating termsAccepted for user with pk: {}", pk);
      userDao.updateTermsAccepted(pk, sk);
      log.info("Successfully updated termsAccepted for user with pk: {}", pk);
    } catch (Exception e) {
      log.error("Error updating termsAccepted: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to update termsAccepted", e);
    }
  }

}
