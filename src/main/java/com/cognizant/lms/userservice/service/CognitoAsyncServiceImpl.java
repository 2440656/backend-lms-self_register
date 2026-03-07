package com.cognizant.lms.userservice.service;

import static com.cognizant.lms.userservice.constants.Constants.DEFAULT_TEMP_PASSWORD_LENGTH;
import static com.cognizant.lms.userservice.constants.Constants.LOWER_CASE_STRING;
import static com.cognizant.lms.userservice.constants.Constants.MAX_BACKOFF_IN_MS;
import static com.cognizant.lms.userservice.constants.Constants.MAX_RETRIES;
import static com.cognizant.lms.userservice.constants.Constants.NUMBER_STRING;
import static com.cognizant.lms.userservice.constants.Constants.RATE_LIMITER_NAME;
import static com.cognizant.lms.userservice.constants.Constants.RATE_LIMITER_PER_SEC;
import static com.cognizant.lms.userservice.constants.Constants.RATE_LIMITER_REFRESH_SECS;
import static com.cognizant.lms.userservice.constants.Constants.RATE_LIMITER_TIMEOUT_SECS;
import static com.cognizant.lms.userservice.constants.Constants.SPECIAL_CHARACTER_STRING;
import static com.cognizant.lms.userservice.constants.Constants.THROTTLING_EXCEPTION;
import static com.cognizant.lms.userservice.constants.Constants.TRACKER_ID;
import static com.cognizant.lms.userservice.constants.Constants.UNEXPECTED_ERROR;
import static com.cognizant.lms.userservice.constants.Constants.UPPER_CASE_STRING;

import com.cognizant.lms.userservice.config.CognitoAsyncConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.exception.CognitoServiceException;
import com.cognizant.lms.userservice.messaging.SqsProducerService;
import com.cognizant.lms.userservice.utils.ImageBase64Converter;
import com.cognizant.lms.userservice.utils.S3Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.PreDestroy;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Slf4j
@Service
public class CognitoAsyncServiceImpl implements CognitoAsyncService {
  private static final SecureRandom secureRandom = new SecureRandom();
  private final CognitoIdentityProviderAsyncClient cognitoAsyncClient;

  private final String userPoolId;
  private final RateLimiter rateLimiter;
  private final String lmsFromEmailAddress;
  private final String lmsUrl;
  private final String lmsAdminEmail;
  private final SqsProducerService sqsProducerService;
  private final S3Util s3Util;
  private final String bucketName;

  public CognitoAsyncServiceImpl(CognitoAsyncConfig cognitoAsyncSQSConfig,
                                 @Value("${AWS_COGNITO_USER_POOL_ID}") String userPoolId,
                                 @Value("${LMS_FROM_EMAIL_ADDRESS}") String lmsFromEmailAddress,
                                 @Value("${LMS_URL}") String lmsUrl,
                                 @Value("${LMS_ADMIN_EMAIL}") String lmsAdminEmail,
                                 SqsProducerService sqsProducerService,
                                 S3Util s3Util,
                                 @Value("${AWS_S3_BUCKET_NAME}") String bucketName) {
    this.cognitoAsyncClient = cognitoAsyncSQSConfig.cognitoAsyncClient();
    this.userPoolId = userPoolId;
    RateLimiterConfig config = RateLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_SECS))
        .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMITER_REFRESH_SECS))
        .limitForPeriod(RATE_LIMITER_PER_SEC)
        .build();
    this.rateLimiter = RateLimiter.of(RATE_LIMITER_NAME, config);
    this.lmsFromEmailAddress = lmsFromEmailAddress;
    this.lmsUrl = lmsUrl;
    this.lmsAdminEmail = lmsAdminEmail;
    this.sqsProducerService = sqsProducerService;
    this.s3Util = s3Util;
    this.bucketName = bucketName;
  }

  @PreDestroy
  public void close() {
    log.info("Shutting down CognitoAsyncService and closing Cognito client");
    cognitoAsyncClient.close();
  }

  public CompletableFuture<Void> createCognitoUserAsync(User user) {
    rateLimiter.acquirePermission();
    return createUserWithRetry(user, MAX_RETRIES);
  }
  public CompletableFuture<Void> recreateCognitoUserAsync(User user,String tempPassword){
    rateLimiter.acquirePermission();
    return recreateUserWithRetry(user,tempPassword,MAX_RETRIES);
  }

  private CompletableFuture<Void> recreateUserWithRetry(User user ,String tempPassword, int maxRetries) {
    return recreateUserAsync(user,tempPassword)
        .exceptionally(ex -> {
              if (maxRetries > 0 && isThrottlingException(ex)) {
                log.warn("Retrying to Recreate user: {}, Retries left: {}",
                    user.getEmailId(), maxRetries);
                return this.retryWithBackoff(user, maxRetries - 1);
              } else {
                return handleFailure(ex, user);
              }
            }
        );
  }

  private CompletableFuture<Void> createUserWithRetry(User user, int maxRetries) {
    return createUserAsync(user)
        .exceptionally(ex -> {
              if (maxRetries > 0 && isThrottlingException(ex)) {
                log.warn("Retrying to create user: {}, Retries left: {}",
                    user.getEmailId(), maxRetries);
                return this.retryWithBackoff(user, maxRetries - 1);
              } else {
                return handleFailure(ex, user);
              }
            }
        );
  }
  @Override
  public CompletableFuture<Void> enableCognitoUserAsync(User user) {
    AdminEnableUserRequest request = AdminEnableUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .build();
    return cognitoAsyncClient.adminEnableUser(request).thenAccept(response -> {
      log.info("Successfully Enabled user: {}", user.getEmailId());
    }).exceptionally(ex -> {
      log.error("Failed to Enable user: {}, Error: {}", user.getEmailId(), ex.getMessage());
      throw new CognitoServiceException("Failed to enable user", UNEXPECTED_ERROR, ex);
    });
  }

  private CompletableFuture<Void> recreateUserAsync(User user,String tempPassword) {
    AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .temporaryPassword(tempPassword)
        .userAttributes(
            AttributeType.builder().name("email").value(user.getEmailId()).build(),
            AttributeType.builder().name("given_name").value(user.getFirstName()).build(),
            AttributeType.builder().name("family_name").value(user.getLastName()).build(),
            AttributeType.builder().name("email_verified").value("true").build())
        .messageAction(MessageActionType.SUPPRESS)
        .desiredDeliveryMediums(DeliveryMediumType.valueOf("EMAIL"))
        .clientMetadata(null)
        .build();
    return cognitoAsyncClient.adminCreateUser(createUserRequest)
        .thenApply(response -> handlereActiveuserSuccess(response, user, tempPassword));
  }

  private CompletableFuture<Void> createUserAsync(User user) {
    return createUserAsync(user, userPoolId);
  }
  private CompletableFuture<Void> createUserAsync(User user,String userPoolId) {
    String tempPassword = this.temporaryPasswordGenerator();
    log.info("Creating Cognito user: email={}, userPoolId={}", user.getEmailId(), userPoolId);
    AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .temporaryPassword(tempPassword)
        .userAttributes(
            AttributeType.builder().name("email").value(user.getEmailId()).build(),
            AttributeType.builder().name("given_name").value(user.getFirstName()).build(),
            AttributeType.builder().name("family_name").value(user.getLastName()).build(),
            AttributeType.builder().name("email_verified").value("true").build())
        .messageAction(MessageActionType.SUPPRESS)
        .desiredDeliveryMediums(DeliveryMediumType.valueOf("EMAIL"))
        .clientMetadata(null)
        .build();
    log.debug("AdminCreateUserRequest: {}", createUserRequest);
    return cognitoAsyncClient.adminCreateUser(createUserRequest)
        .thenApply(response -> handleSuccess(response, user, tempPassword));
  }

  private Void retryWithBackoff(User user, int maxRetries) {
    try {
      long backoffTime = calculateBackoffTime(maxRetries);
      log.warn("Backing off for {} ms before retrying to create user: {}",
          backoffTime, user.getEmailId());
      TimeUnit.MILLISECONDS.sleep(backoffTime);
      createUserWithRetry(user, maxRetries);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted while retrying to create user: {}, Error: {}",
          user.getEmailId(), e.getMessage());
    }
    return null;
  }
  public String temporaryPasswordGenerator() {
    String upperCase = UPPER_CASE_STRING;
    String lowerCase = LOWER_CASE_STRING;
    String number = NUMBER_STRING;
    String specialString = SPECIAL_CHARACTER_STRING;
    List<Character> passwordChars = Stream.of(
        upperCase.charAt(secureRandom.nextInt(upperCase.length())),
        lowerCase.charAt(secureRandom.nextInt(lowerCase.length())),
        number.charAt(secureRandom.nextInt(number.length())),
        specialString.charAt(secureRandom.nextInt(specialString.length()))
    ).collect(Collectors.toList());

    String allChars = upperCase + lowerCase + number + specialString;
    passwordChars.addAll(IntStream.range(4, DEFAULT_TEMP_PASSWORD_LENGTH)
        .mapToObj(i -> allChars.charAt(secureRandom.nextInt(allChars.length())))
        .toList());

    Collections.shuffle(passwordChars, secureRandom);

    return passwordChars.stream()
        .map(String::valueOf)
        .collect(Collectors.joining());
  }

  @Override
  public CompletableFuture<Void> disableCognitoUserAsync(User user) {
    AdminDisableUserRequest request = AdminDisableUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .build();
    return cognitoAsyncClient.adminDisableUser(request).thenAccept(response -> {
      log.info("Successfully disabled user: {}", user.getEmailId());
    }).exceptionally(ex -> {
      log.error("Failed to disable user: {}, Error: {}", user.getEmailId(), ex.getMessage());
      throw new CognitoServiceException("Failed to disable user", UNEXPECTED_ERROR, ex);
    });
  }
  @Override
  public CompletableFuture<Void> disableCognitoUserAsyncWithEmail(String emailId) {
    AdminDisableUserRequest request = AdminDisableUserRequest.builder()
        .userPoolId(userPoolId)
        .username(emailId)
        .build();
    return cognitoAsyncClient.adminDisableUser(request).thenAccept(response -> {
      log.info("Successfully disabled user: {}", emailId);
    }).exceptionally(ex -> {
      log.error("Failed to disable user: {}, Error: {}", emailId, ex.getMessage());
      throw new CognitoServiceException("Failed to disable user", UNEXPECTED_ERROR, ex);
    });
  }


  @Override
  public CompletableFuture<Void> enableCognitoUserAsyncWithEMail(String emailId) {
    AdminEnableUserRequest request = AdminEnableUserRequest.builder()
        .userPoolId(userPoolId)
        .username(emailId)
        .build();
    log.info("Enabling Cognito user: {}", emailId);
    return cognitoAsyncClient.adminEnableUser(request)
        .thenAccept(response -> log.info("User enabled in Cognito: {}", emailId));
  }

  private long calculateBackoffTime(int maxRetries) {
    long exponentialBackoffTime = (long) Math.pow(2, maxRetries) * 100;
    long jitter = (long) (Math.random() * 500);
    return Math.min(exponentialBackoffTime + jitter, MAX_BACKOFF_IN_MS);
  }

  private boolean isThrottlingException(Throwable ex) {
    return ex instanceof CognitoIdentityProviderException exception
        && exception.awsErrorDetails().errorCode().equals(THROTTLING_EXCEPTION);
  }

  private Void handleSuccess(AdminCreateUserResponse response, User user, String tempPassword) {
    log.info("Successfully created user: {}", user.getEmailId());
    sendMessageToSqs(user, tempPassword,null).join();
    return null;
  }
  private Void handlereActiveuserSuccess(AdminCreateUserResponse response, User user, String tempPassword) {
    log.info("Successfully recreated user: {}", user.getEmailId());
    sendMessageToSqs(user, tempPassword,null).join();
    return null;
  }


  private CompletableFuture<Void> sendMessageToSqs(User user, String tempPassword, String ccEmail) {
    return CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.SUBJECT_VALUE);
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.SKILLSPRING_USER_WELCOME_NOTIFICATION);
      if (ccEmail != null && !ccEmail.isEmpty()) {
        emailDetails.put(Constants.CC, ccEmail);
      }

      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      try {
        Resource imageResource =
            s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE,
                bucketName, false);
        String base64Image =
            ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());
        Resource templateResource =
            s3Util.downloadFileFromS3(Constants.WELCOME_EMAIL_TEMPLATE_NAME, Constants.FILE_TYPE_TEMPLATE,
                bucketName, false);
        String templateBody =
            new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);


        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{First_Name}}", user.getFirstName())
            .replace("{{Last_Name}}", user.getLastName())
            .replace("{{User_Name}}", user.getEmailId())
            .replace("{{Password}}", tempPassword)
            .replace("{{LMS_Application_Url}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier())
            .replace("{{Support_Email}}", lmsAdminEmail)
            .replace("{{Year}}", currentYear);
        log.info("Generating Welcome Email body For User:{} in sqs, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading template file: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending message to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }



  @Override
  public CompletableFuture<Void> sendWelcomeMailToSocialIDP(User user) {
    return CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.SUBJECT_VALUE);
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.SOCIAL_IDP_USER_WELCOME_NOTIFICATION);
      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      try {
        Resource imageResource =
            s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE,
                bucketName, false);
        String base64Image =
            ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());
        Resource templateResource =
            s3Util.downloadFileFromS3(Constants.WELCOME_EMAIL_TEMPLATE_NAME_SOCIAL_IDP, Constants.FILE_TYPE_TEMPLATE,
                bucketName, false);
        String templateBody =
            new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);


        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{First_Name}}", user.getFirstName())
            .replace("{{Last_Name}}", user.getLastName())
            .replace("{{LMS_Application_Url}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier())
            .replace("{{Support_Email}}", lmsAdminEmail)
            .replace("{{Year}}", currentYear);
        log.info("Generating Welcome Email body For user:{} in Social IDP, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading template file: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending message to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }
  private void setTrackerId(Map<String, String> emailDetails){
        String trackerId = UUID.randomUUID().toString();
        emailDetails.put(TRACKER_ID, trackerId);
  }







  private Void handleFailure(Throwable ex, User user) {
    if (ex instanceof CognitoIdentityProviderException exception) {
      log.error("Failed to create user: {}", exception.awsErrorDetails().errorMessage());
      throw new CognitoServiceException(exception.awsErrorDetails()
          .errorMessage(), exception.awsErrorDetails().errorCode(), ex);
    } else {
      log.error("Unexpected error occurred while creating user: {}, Error: {}",
          user.getEmailId(), ex.getMessage());
      throw new CognitoServiceException(ex.getMessage(), UNEXPECTED_ERROR, ex);
    }
  }

  @Override
  public CompletableFuture<Void> deleteCognitoUserAsync(User user) {
    rateLimiter.acquirePermission();
    return deleteUserWithRetry(user, MAX_RETRIES);
  }

  private CompletableFuture<Void> deleteUserWithRetry(User user, int maxRetries) {
    return deleteUserAsync(user)
        .exceptionally(ex -> {
              if (maxRetries > 0 && isThrottlingException(ex)) {
                log.warn("Retrying to delete user: {}, Retries left: {}",
                    user.getEmailId(), maxRetries);
                return this.retryWithBackoff(user, maxRetries - 1);
              } else {
                return handleDeleteUserFailure(ex, user);
              }
            }
        );
  }

  @Override
  public CompletableFuture<Void> deleteBulkCognitoUserAsync(User user) {
    rateLimiter.acquirePermission();
    return deleteBulkUserWithRetry(user, MAX_RETRIES);
  }

  private CompletableFuture<Void> deleteBulkUserWithRetry(User user, int maxRetries) {
    return deleteBulkUserAsync(user)
        .exceptionally(ex -> {
              if (maxRetries > 0 && isThrottlingException(ex)) {
                log.warn("Retrying to delete user: {}, Retries left: {}",
                    user.getEmailId(), maxRetries);
                return this.retryWithBackoff(user, maxRetries - 1);
              } else {
                return handleDeleteUserFailure(ex, user);
              }
            }
        );
  }

  public CompletableFuture<Void> deleteUserAsync(User user) {
    AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())

        .build();

    return cognitoAsyncClient.adminDeleteUser(deleteUserRequest)
        .thenApply(response -> handleDeleteUserSuccess(response, user));
  }

  public CompletableFuture<Void> deleteBulkUserAsync(User user) {
    AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .build();

    return cognitoAsyncClient.adminDeleteUser(deleteUserRequest)
        .thenApply(response -> {
          log.info("Successfully deleted user: {}", user.getEmailId());
          return null;
        });
  }

  private Void handleDeleteUserSuccess(AdminDeleteUserResponse response, User user) {
    log.info("Successfully deleted user: {}", user.getEmailId());
    createUserAsync(user).join();
    return null;
  }

  private Void handleDeleteUserFailure(Throwable ex, User user) {
    if (ex instanceof CognitoIdentityProviderException exception) {
      log.error("Failed to delete user: {}", exception.awsErrorDetails().errorMessage());
      throw new CognitoServiceException(exception.awsErrorDetails()
          .errorMessage(), exception.awsErrorDetails().errorCode(), ex);
    } else {
      log.error("Unexpected error occurred while deleting user: {}, Error: {}",
          user.getEmailId(), ex.getMessage());
      throw new CognitoServiceException(ex.getMessage(), UNEXPECTED_ERROR, ex);
    }
  }


  @Override
  public CompletableFuture<Void> updateCognitoUserAsync(User user) {
    rateLimiter.acquirePermission();
    return updateUserWithRetry(user, MAX_RETRIES);
  }


  private CompletableFuture<Void> updateUserWithRetry(User user, int maxRetries) {
    return updateUserAsync(user)
        .exceptionally(ex -> {
          if (maxRetries > 0 && isThrottlingException(ex)) {
            log.warn("Retrying to update user: {}, Retries left: {}", user.getEmailId(),
                maxRetries);
            return this.retryWithBackoff(user, maxRetries - 1);
          } else {
            return handleUpdateUserFailure(ex, user);
          }
        });
  }

  private CompletableFuture<Void> updateUserAsync(User user) {
    AdminUpdateUserAttributesRequest updateUserRequest = AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .userAttributes(
            AttributeType.builder().name("given_name").value(user.getFirstName()).build(),
            AttributeType.builder().name("family_name").value(user.getLastName()).build(),
            AttributeType.builder().name("email").value(user.getEmailId()).build()
        )
        .build();

    return cognitoAsyncClient.adminUpdateUserAttributes(updateUserRequest)
        .thenApply(response -> handleUpdateUserSuccess(response, user));
  }

  private Void handleUpdateUserSuccess(AdminUpdateUserAttributesResponse response, User user) {
    log.info("Successfully updated user: {}", user.getEmailId());
    return null;
  }

  private Void handleUpdateUserFailure(Throwable ex, User user) {
    if (ex instanceof CognitoIdentityProviderException exception) {
      log.error("Failed to update user: {}", exception.awsErrorDetails().errorMessage());
      throw new CognitoServiceException(exception.awsErrorDetails().errorMessage(),
          exception.awsErrorDetails().errorCode(), ex);
    } else {
      log.error("Unexpected error occurred while updating user: {}, Error: {}", user.getEmailId(),
          ex.getMessage());
      throw new CognitoServiceException(ex.getMessage(), UNEXPECTED_ERROR, ex);
    }
  }

  @Override
  public String sendWelcomeEmail(User user) throws Exception {
    try {
      String loginOption = user.getLoginOption();
      log.info("Login option is {}", loginOption);
      if (loginOption != null && loginOption.equalsIgnoreCase(Constants.LOGIN_OPTION_SOCIAL_IDP)) {
        log.info("Sending welcome email to the Social IDP user {}", user.getEmailId());
        sendWelcomeMailToSocialIDP(user);
        return "Welcome mail has been re-sent to the Social IDP user successfully.";
      }
      AdminGetUserResponse response = getUserFromCognito(user.getEmailId());
      if (response != null) {
        log.info("User exists in Cognito {} ", user.getEmailId());
        if (response.userStatus().equals(UserStatusType.CONFIRMED)) {
          log.info("User is already confirmed in Cognito {}", user.getEmailId());
          return "User is already confirmed in Cognito";
        } else {
          log.info("User is not confirmed in Cognito {}", user.getEmailId());
          AdminDeleteUserResponse deleteResponse = deleteCognitoUser(user);
          if (deleteResponse.sdkHttpResponse().isSuccessful()) {
            log.info("User deleted successfully from Cognito {}", user.getEmailId());
            createCognitoUser(user);
            return "Welcome mail has been re-sent to the user successfully.";
          }
          return "Error occurred while re sending email to the user.";
        }
      } else {
        log.info("User does not exist Cognito {}", user.getEmailId());
        return "User does not exist in Cognito";
      }
    } catch (CognitoIdentityProviderException e) {
      log.error("Error occurred while resending welcome email: {}", e.getMessage());
      throw new CognitoServiceException("Error occurred while resending welcome email: ",
          UNEXPECTED_ERROR, e);
    }
  }

  private void createCognitoUser(User user) {
    String tempPassword = this.temporaryPasswordGenerator();
    AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .temporaryPassword(tempPassword)
        .userAttributes(
            AttributeType.builder().name("email").value(user.getEmailId()).build(),
            AttributeType.builder().name("given_name").value(user.getFirstName()).build(),
            AttributeType.builder().name("family_name").value(user.getLastName()).build(),
            AttributeType.builder().name("email_verified").value("true").build())
        .messageAction(MessageActionType.SUPPRESS)
        .desiredDeliveryMediums(DeliveryMediumType.valueOf("EMAIL"))
        .clientMetadata(null)
        .build();
    AdminCreateUserResponse response = cognitoAsyncClient.adminCreateUser(createUserRequest).join();
    if (response.sdkHttpResponse().isSuccessful()) {
      log.info("User created successfully in Cognito {}", user.getEmailId());
      sendMessageToSqs(user, tempPassword,lmsAdminEmail).join();
      log.info("Welcome email sent to the user successfully {}", user.getEmailId());
    }
  }

  private AdminDeleteUserResponse deleteCognitoUser(User user) throws Exception {
    AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmailId())
        .build();
    return cognitoAsyncClient.adminDeleteUser(deleteUserRequest).join();
  }

  public AdminGetUserResponse getUserFromCognito(String email) {
    AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
        .userPoolId(userPoolId)
        .username(email)
        .build();
    CompletableFuture<AdminGetUserResponse> getUserResponseFuture =
        cognitoAsyncClient.adminGetUser(getUserRequest);
    AdminGetUserResponse response = getUserResponseFuture.join();
    log.info("getting user response from cognito {}",response);
    return response;
  }

  public void sendRoleAssignmentEmail(User user, String roleName) {
    CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.NEW_ROLE_ASSIGNMENT_TO_USER);
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.NEW_ROLE_ASSIGNMENT_NOTIFICATION);
      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      try {
        Resource imageResource =
            s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE,
                bucketName, false);
        String base64Image =
            ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());

        Resource templateResource =
            s3Util.downloadFileFromS3(Constants.ROLE_ASSIGNMENT_EMAIL_TEMPLATE, Constants.FILE_TYPE_TEMPLATE, bucketName, false);
        String templateBody =
            new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);

        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{User_Full_Name}}", user.getFirstName() + " " + user.getLastName())
            .replace("{{Help_Resource_Link}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier() + Constants.HELP_RESOURCE_URL)
            .replace("{{User_Role}}", roleName)
            .replace("{{Year}}", currentYear)
            .replace("{{Support_Email}}", lmsAdminEmail);

        log.info("Generating Role Assignment Email body for user: {}, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading role assignment email template: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending role assignment email to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting role assignment email message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }

  private CompletableFuture<Void> setUserPasswordWithRetry(String userName, String password, User user, int maxRetries) {
    log.info("calling setUserPasswordWithRetry");
    return setUserPasswordAsync(userName, password, user)
        .exceptionally(ex -> {
          if (maxRetries > 0 && isThrottlingException(ex)) {
            log.warn("Retrying to set password for user: {}, Retries left: {}", userName, maxRetries);
            return this.retryWithBackoff(userName, password, user, maxRetries - 1);
          } else {
            log.error("Failed to set password for user: {}, Error: {}", userName, ex.getMessage());
            throw new RuntimeException("Failed to set password", ex);
          }
        });
  }

  private Void retryWithBackoff(String userName, String password, User user, int maxRetries) {
    try {
      long backoffTime = calculateBackoffTime(maxRetries);
      log.warn("Backing off for {} ms before retrying to set password for user: {}", backoffTime, userName);
      TimeUnit.MILLISECONDS.sleep(backoffTime);
      setUserPasswordWithRetry(userName, password, user, maxRetries).join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted while retrying to set password for user: {}, Error: {}", userName, e.getMessage());
    }
    return null;
  }

  public CompletableFuture<Void> setCognitoUserPasswordAsync(String userName, String password, User user) {
    log.info("Calling setCognitoUserPasswordAsync");
    rateLimiter.acquirePermission();
    return setUserPasswordWithRetry(userName, password, user, MAX_RETRIES);
  }



  private CompletableFuture<Void> setUserPasswordAsync(String userName, String password, User user) {
    log.info("Calling setUserPasswordAsync");
    AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
        .userPoolId(userPoolId)
        .username(userName)
        .password(password)
        .permanent(false)
        .build();
    log.info("Password is set in cognito: {}", password);

    return cognitoAsyncClient.adminSetUserPassword(setPasswordRequest)
        .thenApply(response -> handleSetUserPasswordSuccess(response, user))
        .exceptionally(ex -> {
          log.error("Error setting password for user: {}, Error: {}", userName, ex.getMessage(), ex);
          return null;
        });
  }

  private Void handleSetUserPasswordSuccess(AdminSetUserPasswordResponse response, User user) {
    log.info("Successfully set permanent password for user: {}", user.getEmailId());
    return null;
  }

  public void sendTemporaryPasswordEmail(User user, String tempPassword) {
    CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.TEMP_PASSWORD_REMINDER_SUBJECT);
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.TEMP_PASSWORD_NOTIFICATION);
      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      String todayDate = String.valueOf(java.time.LocalDate.now().getDayOfMonth());
      try {
        Resource imageResource = s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE, bucketName, false);
        String base64Image = ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());
        Resource templateResource = s3Util.downloadFileFromS3(Constants.TEMP_PASSWORD_TEMPLATE_NAME, Constants.FILE_TYPE_TEMPLATE, bucketName, false);
        String templateBody = new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);
        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{User_Full_Name}}", user.getFirstName() + " " + user.getLastName())
            .replace("{{User_Name}}", user.getEmailId())
            .replace("{{Password}}", tempPassword)
            .replace("{{LMS_Application_Url}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier())
            .replace("{{Support_Email}}", lmsAdminEmail)
            .replace("{{Year}}", currentYear)
            .replace("{{TodayDate}}", todayDate);
        log.info("Generating Temporary Password email notification for user: {}, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading template file: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending message to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }

  public void sendReactivateSkillSpringUserEmail(User user, String tempPassword) {
    CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.REACTIVATE_SKILLSPRING_USER_SUBJECT );
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.REACTIVATE_SKILLSPRING_USER_NOTIFICATION);
      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      try {
        Resource imageResource = s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE, bucketName, false);
        String base64Image = ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());
        Resource templateResource = s3Util.downloadFileFromS3(Constants.REACTIVATE_SKILLSPRING_USER_EMAIL_TEMPLATE_NAME, Constants.FILE_TYPE_TEMPLATE, bucketName, false);
        String templateBody = new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);
        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{User_Full_Name}}", user.getFirstName() + " " + user.getLastName())
            .replace("{{User_Name}}", user.getEmailId())
            .replace("{{Password}}", tempPassword)
            .replace("{{LMS_Application_Url}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier())
            .replace("{{Support_Email}}", lmsAdminEmail)
            .replace("{{Year}}", currentYear);
        log.info("Generating Reactivate SkillSpring User  email notification for user: {}, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading template file Reactivate SkillSpring Use: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending Reactivate SkillSpring User message to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting Reactivate SkillSpring User message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }

  public void sendReactivateUserEmail(User user) {
    CompletableFuture.runAsync(() -> {
      Map<String, String> emailDetails = new HashMap<>();
      setTrackerId(emailDetails);
      emailDetails.put(Constants.TO, user.getEmailId());
      emailDetails.put(Constants.FROM, lmsFromEmailAddress);
      emailDetails.put(Constants.SUBJECT, Constants.REACTIVATE_USER_SUBJECT);
      emailDetails.put(Constants.NOTIFICATION_EVENT_NAME,Constants.REACTIVATE_USER_NOTIFICATION);
      String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
      try {
        Resource imageResource = s3Util.downloadFileFromS3(Constants.IMAGE_NAME, Constants.FILE_TYPE_IMAGE, bucketName, false);
        String base64Image = ImageBase64Converter.convertImageToBase64(imageResource.getContentAsByteArray());
        Resource templateResource = s3Util.downloadFileFromS3(Constants.REACTIVATE_USER_EMAIL_TEMPLATE_NAME, Constants.FILE_TYPE_TEMPLATE, bucketName, false);
        String templateBody = new String(templateResource.getContentAsByteArray(), StandardCharsets.UTF_8);
        String body = templateBody.replace("{{Image}}", base64Image)
            .replace("{{Image_Width}}", Constants.IMAGE_WIDTH)
            .replace("{{User_Full_Name}}", user.getFirstName() + " " + user.getLastName())
            .replace("{{LMS_Application_Url}}", "https://" + TenantUtil.getTenantDetails().getTenantIdentifier())
            .replace("{{Support_Email}}", lmsAdminEmail)
            .replace("{{Year}}", currentYear);

        log.info("Generating Reactivate User Email notification for user: {}, TrackerId: {}", user.getPk(), emailDetails.get(TRACKER_ID));
        emailDetails.put(Constants.BODY, body);
      } catch (Exception e) {
        log.error("Error occurred while reading template file Reactivate User: {}", e.getMessage());
      }
      Map<String, String> clientDetails = new HashMap<>();
      clientDetails.put(Constants.CLIENT_ID, Constants.CLIENT_ID_VALUE);
      Map<String, Object> message = new HashMap<>();
      message.put(Constants.EMAIL_DETAILS, emailDetails);
      message.put(Constants.CLIENT_DETAILS, clientDetails);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        SendMessageResponse sendMessageResponse = sqsProducerService.sendMessage(jsonMessage);
        log.info("Processing SQS message TrackerId: {}, Message ID: {}", emailDetails.get(TRACKER_ID), sendMessageResponse.messageId());
      } catch (SqsException e) {
        log.error("Error occurred while sending Reactivate User message to SQS for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      } catch (JsonProcessingException e) {
        log.error("Error occurred while converting Reactivate User message to JSON for TrackerId: {}, Error: {}", emailDetails.get(TRACKER_ID), e.getMessage());
      }
    });
  }
  public CompletableFuture<Void> createUserWithPoolIdAsync(User user, String userPoolId) {
      return createUserAsync(user, userPoolId);
  }

  @Override
  public CompletableFuture<Void> signUpUserAsync(String email, String password, String firstName,
                           String lastName, String appClientId,
                           String appClientSecret) {
    SignUpRequest signUpRequest = SignUpRequest.builder()
        .clientId(appClientId)
        .username(email)
        .password(password)
      .secretHash(resolveSecretHash(email, appClientId, appClientSecret))
        .userAttributes(
            AttributeType.builder().name("email").value(email).build(),
            AttributeType.builder().name("given_name").value(firstName).build(),
            AttributeType.builder().name("family_name").value(lastName).build())
        .build();

    return cognitoAsyncClient.signUp(signUpRequest)
        .thenAccept(response -> log.info("Cognito sign-up initiated for email: {}", email))
        .exceptionally(ex -> {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          if (cause instanceof CognitoIdentityProviderException exception) {
            throw new CognitoServiceException(exception.awsErrorDetails().errorMessage(),
                exception.awsErrorDetails().errorCode(), cause);
          }
          throw new CognitoServiceException(cause.getMessage(), UNEXPECTED_ERROR, cause);
        });
  }

  @Override
  public CompletableFuture<Void> confirmSignUpAsync(String email, String otpCode,
                                                    String appClientId,
                                                    String appClientSecret) {
    ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
        .clientId(appClientId)
        .username(email)
        .confirmationCode(otpCode)
        .secretHash(resolveSecretHash(email, appClientId, appClientSecret))
        .build();

    return cognitoAsyncClient.confirmSignUp(confirmSignUpRequest)
        .thenAccept(response -> log.info("Cognito OTP verification completed for email: {}", email))
        .exceptionally(ex -> {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          if (cause instanceof CognitoIdentityProviderException exception) {
            throw new CognitoServiceException(exception.awsErrorDetails().errorMessage(),
                exception.awsErrorDetails().errorCode(), cause);
          }
          throw new CognitoServiceException(cause.getMessage(), UNEXPECTED_ERROR, cause);
        });
  }

  @Override
  public CompletableFuture<Void> resendSignUpOtpAsync(String email, String appClientId,
                                                       String appClientSecret) {
    ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
        .clientId(appClientId)
        .username(email)
        .secretHash(resolveSecretHash(email, appClientId, appClientSecret))
        .build();

    return cognitoAsyncClient.resendConfirmationCode(resendRequest)
        .thenAccept(response -> log.info("Cognito OTP resend initiated for email: {}", email))
        .exceptionally(ex -> {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          if (cause instanceof CognitoIdentityProviderException exception) {
            throw new CognitoServiceException(exception.awsErrorDetails().errorMessage(),
                exception.awsErrorDetails().errorCode(), cause);
          }
          throw new CognitoServiceException(cause.getMessage(), UNEXPECTED_ERROR, cause);
        });
  }

  private String resolveSecretHash(String username, String appClientId, String appClientSecret) {
    if (appClientSecret == null || appClientSecret.trim().isEmpty()) {
      return null;
    }
    return generateSecretHash(username, appClientId, appClientSecret);
  }

  private String generateSecretHash(String username, String appClientId, String appClientSecret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(appClientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);
      mac.update(username.getBytes(StandardCharsets.UTF_8));
      byte[] rawHmac = mac.doFinal(appClientId.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(rawHmac);
    } catch (Exception ex) {
      throw new CognitoServiceException("Unable to compute client secret hash", UNEXPECTED_ERROR,
          ex);
    }
  }
}