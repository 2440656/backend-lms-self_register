package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cognizant.lms.userservice.config.CognitoAsyncConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.exception.CognitoServiceException;
import com.cognizant.lms.userservice.messaging.SqsProducerService;
import com.cognizant.lms.userservice.utils.S3Util;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
public class CognitoAsyncServiceImplTest {

  @Mock
  private CognitoIdentityProviderAsyncClient cognitoAsyncClient;

  @Mock
  private CognitoAsyncConfig cognitoAsyncSQSConfig;

  @Mock
  private SqsProducerService sqsProducerService;

  @Mock
  private S3Util s3Util;

  private String userPoolId;
  private String lmsFromEmailAddress;
  private String lmsUrl;
  private String lmsAdminEmail;
  private String bucketName;

  @InjectMocks
  private CognitoAsyncServiceImpl cognitoAsyncService;
  private User testUser;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    userPoolId = "userPoolId";
    lmsFromEmailAddress = "lmsFromEmailAddress";
    lmsUrl = "lmsUrl";
    lmsAdminEmail = "lmsAdminEmail";
    bucketName = "bucketName";
    testUser = new User();
    testUser.setEmailId("test@example.com");

    when(cognitoAsyncSQSConfig.cognitoAsyncClient()).thenReturn(cognitoAsyncClient);

    cognitoAsyncService = new CognitoAsyncServiceImpl(
        cognitoAsyncSQSConfig,
        userPoolId,
        lmsFromEmailAddress,
        lmsUrl,
        lmsAdminEmail,
        sqsProducerService,
        s3Util,
        bucketName
    );
  }

  @Test
  public void testCreateCognitoUserAsync_Success() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    user.setFirstName("Test");
    user.setLastName("User");
    AdminCreateUserResponse response = AdminCreateUserResponse.builder().build();
    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(response));
    SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
    when(sqsProducerService.sendMessage(anyString())).thenReturn(sendMessageResponse);
    Resource resource = mock(Resource.class);
    when(s3Util.downloadFileFromS3(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(resource);
    CompletableFuture<Void> future = cognitoAsyncService.createCognitoUserAsync(user);
    assertDoesNotThrow(future::join);
    verify(cognitoAsyncClient, times(1))
        .adminCreateUser(any(AdminCreateUserRequest.class));
  }


//  @Test
//  void testDeleteCognitoUserAsync_Success() {
//    User user = new User();
//    user.setEmailId("test@example.com");
//    AdminDeleteUserResponse response = AdminDeleteUserResponse.builder().build();
//    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
//        .thenReturn(CompletableFuture.completedFuture(response));
//    AdminCreateUserResponse response1 = AdminCreateUserResponse.builder().build();
//    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
//        .thenReturn(CompletableFuture.completedFuture(response1));
//    SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
//    when(sqsProducerService.sendMessage(anyString())).thenReturn(sendMessageResponse);
//    CompletableFuture<Void> future = cognitoAsyncService.deleteCognitoUserAsync(user);
//    assertDoesNotThrow(future::join);
//    verify(cognitoAsyncClient, times(1))
//        .adminDeleteUser(any(AdminDeleteUserRequest.class));
//  }
@Test
void testDeleteCognitoUserAsync_Success() {
  User user = new User();
  user.setEmailId("test@example.com");

  // Mock the adminDeleteUser response
  AdminDeleteUserResponse deleteResponse = AdminDeleteUserResponse.builder().build();
  when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(deleteResponse));

  // Mock the adminCreateUser response
  AdminCreateUserResponse createResponse = AdminCreateUserResponse.builder().build();
  when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(createResponse));

  // Mock the sendMessage response
  SendMessageResponse sendMessageResponse = SendMessageResponse.builder()
          .messageId("mockMessageId")
          .build();
  when(sqsProducerService.sendMessage(anyString())).thenReturn(sendMessageResponse);

  // Execute the method and assert no exceptions are thrown
  CompletableFuture<Void> future = cognitoAsyncService.deleteCognitoUserAsync(user);
  assertDoesNotThrow(future::join);

  // Verify interactions
  verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
  verify(cognitoAsyncClient, times(1)).adminCreateUser(any(AdminCreateUserRequest.class));
  verify(sqsProducerService, times(1)).sendMessage(anyString());
}

  @Test
  void testDeleteBulkCognitoUserAsync_Success() {
    User user = new User();
    user.setEmailId("test@example.com");

    AdminDeleteUserResponse deleteResponse = AdminDeleteUserResponse.builder().build();
    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteResponse));

    CompletableFuture<Void> future = cognitoAsyncService.deleteBulkCognitoUserAsync(user);
    assertDoesNotThrow(future::join);

    verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
  }

  @Test
  void testDeleteBulkUserAsync_Success() {
    AdminDeleteUserResponse deleteResponse = AdminDeleteUserResponse.builder().build();
    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteResponse));

    CompletableFuture<Void> future = cognitoAsyncService.deleteBulkUserAsync(testUser);
    assertDoesNotThrow(future::join);

    verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
  }

  @Test
  void testDeleteUserAsync_Success() {
    // Mock successful deletion response
    AdminDeleteUserResponse deleteResponse = AdminDeleteUserResponse.builder().build();
    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(deleteResponse));

    // Mock successful creation response (required because createUserAsync is called internally)
    AdminCreateUserResponse createResponse = AdminCreateUserResponse.builder().build();
    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(createResponse));
    SendMessageResponse sendMessageResponse = SendMessageResponse.builder()
            .messageId("mockMessageId")
            .build();
    when(sqsProducerService.sendMessage(anyString())).thenReturn(sendMessageResponse);

    // Call the method and verify no exceptions are thrown
    CompletableFuture<Void> future = cognitoAsyncService.deleteUserAsync(testUser);
    assertDoesNotThrow(future::join);

    // Verify the interactions with the mocks
    verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
    verify(cognitoAsyncClient, times(1)).adminCreateUser(any(AdminCreateUserRequest.class));
    verify(sqsProducerService, times(1)).sendMessage(anyString());
  }

  @Test
  void testUpdateCognitoUserAsync_Success() {
    User user = new User();
    user.setEmailId("test@example.com");
    user.setFirstName("Test");
    user.setLastName("User");
    AdminUpdateUserAttributesResponse response =
        AdminUpdateUserAttributesResponse.builder().build();
    when(cognitoAsyncClient.adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(response));
    CompletableFuture<Void> future = cognitoAsyncService.updateCognitoUserAsync(user);
    assertDoesNotThrow(future::join);
    verify(cognitoAsyncClient, times(1))
        .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));
  }

  @Test
  void testSendWelcomeEmail_UserExists() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    AdminGetUserResponse getUserResponse = AdminGetUserResponse.builder()
        .userStatus(UserStatusType.CONFIRMED)
        .build();
    when(cognitoAsyncClient.adminGetUser(any(AdminGetUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(getUserResponse));
    String result = cognitoAsyncService.sendWelcomeEmail(user);
    assertEquals("User is already confirmed in Cognito", result);
    verify(cognitoAsyncClient, times(1)).adminGetUser(any(AdminGetUserRequest.class));
  }


  @Test
  void testCreateUserWithRetry_RetryOnThrottlingException() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    CognitoIdentityProviderException throttlingException =
        (CognitoIdentityProviderException) CognitoIdentityProviderException.builder()
            .message("ThrottlingException")
            .statusCode(400)
            .build();

    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
        .thenReturn(CompletableFuture.failedFuture(throttlingException))
        .thenReturn(CompletableFuture.completedFuture(AdminCreateUserResponse.builder().build()));
    cognitoAsyncService.createCognitoUserAsync(user);
  }

  @Test
  void testCreateUserWithRetry_FailureAfterMaxRetries() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    CognitoIdentityProviderException throttlingException =
        (CognitoIdentityProviderException) CognitoIdentityProviderException.builder()
            .message("Rate exceeded")
            .statusCode(400)
            .build();
    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
        .thenReturn(CompletableFuture.failedFuture(throttlingException));
    Method method = CognitoAsyncServiceImpl.class
        .getDeclaredMethod("createUserWithRetry", User.class, int.class);
    method.setAccessible(true);
    assertDoesNotThrow(() -> method.invoke(cognitoAsyncService, user, 3));
  }

  @Test
  void testSendWelcomeEmail_UserNotConfirmed_DeleteFailed() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");

    AdminGetUserResponse getUserResponse = AdminGetUserResponse.builder()
        .userStatus(UserStatusType.UNCONFIRMED)
        .build();
    AdminDeleteUserResponse deleteUserResponse =
        (AdminDeleteUserResponse) AdminDeleteUserResponse.builder()
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
            .build();

    when(cognitoAsyncClient.adminGetUser(any(AdminGetUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(getUserResponse));
    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteUserResponse));

    String result = cognitoAsyncService.sendWelcomeEmail(user);
    assertEquals("Error occurred while re sending email to the user.", result);
    verify(cognitoAsyncClient, times(1)).adminGetUser(any(AdminGetUserRequest.class));
    verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
  }

  @Test
  void testSendWelcomeEmail_UserDoesNotExist() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");

    when(cognitoAsyncClient.adminGetUser(any(AdminGetUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    String result = cognitoAsyncService.sendWelcomeEmail(user);
    assertEquals("User does not exist in Cognito", result);
    verify(cognitoAsyncClient, times(1)).adminGetUser(any(AdminGetUserRequest.class));
  }

  @Test
  void testSendWelcomeEmail_UserDeletedAndCreated() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");

    AdminGetUserResponse getUserResponse = AdminGetUserResponse.builder()
        .userStatus(UserStatusType.UNCONFIRMED)
        .build();
    AdminDeleteUserResponse deleteUserResponse =
        (AdminDeleteUserResponse) AdminDeleteUserResponse.builder()
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
            .build();
    AdminCreateUserResponse createUserResponse =
        (AdminCreateUserResponse) AdminCreateUserResponse.builder()
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
            .build();

    when(cognitoAsyncClient.adminGetUser(any(AdminGetUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(getUserResponse));
    when(cognitoAsyncClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteUserResponse));
    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(createUserResponse));
    SendMessageResponse sendMessageResponse = SendMessageResponse.builder().build();
    doReturn(sendMessageResponse).when(sqsProducerService).sendMessage(anyString());

    String result = cognitoAsyncService.sendWelcomeEmail(user);
    assertEquals("Welcome mail has been re-sent to the user successfully.", result);
    verify(cognitoAsyncClient, times(1)).adminGetUser(any(AdminGetUserRequest.class));
    verify(cognitoAsyncClient, times(1)).adminDeleteUser(any(AdminDeleteUserRequest.class));
    verify(cognitoAsyncClient, times(1)).adminCreateUser(any(AdminCreateUserRequest.class));
    verify(sqsProducerService, times(1)).sendMessage(anyString());
  }

  @Test
  void testSendWelcomeEmail_CognitoIdentityProviderException() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    CognitoIdentityProviderException exception =
        (CognitoIdentityProviderException) CognitoIdentityProviderException.builder()
            .message("Error occurred")
            .build();
    when(cognitoAsyncClient.adminGetUser(any(AdminGetUserRequest.class)))
        .thenThrow(exception);
    CognitoServiceException thrown = assertThrows(CognitoServiceException.class, () -> {
      cognitoAsyncService.sendWelcomeEmail(user);
    });
    assertEquals("Error occurred while resending welcome email: ", thrown.getMessage());
    verify(cognitoAsyncClient, times(1)).adminGetUser(any(AdminGetUserRequest.class));
  }

  @Test
  void testClose() {
    cognitoAsyncService.close();
    verify(cognitoAsyncClient, times(1)).close();
  }

  @Test
  void testCreateUserWithRetry_ThrottlingException() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    CognitoIdentityProviderException throttlingException =
        (CognitoIdentityProviderException) CognitoIdentityProviderException.builder()
            .message("Rate exceeded")
            .statusCode(400)
            .build();
    when(cognitoAsyncClient.adminCreateUser(any(AdminCreateUserRequest.class)))
        .thenReturn(CompletableFuture.failedFuture(throttlingException));
    CompletableFuture<Void> future = cognitoAsyncService.createCognitoUserAsync(user);
    CompletionException completionException = assertThrows(CompletionException.class, future::join);
    assertTrue(completionException.getCause() instanceof CognitoServiceException);
    verify(cognitoAsyncClient, times(1)).adminCreateUser(any(AdminCreateUserRequest.class));
  }

  @Test
  void testSendWelcomeEmail_SocialIDP() throws Exception {
    User user = new User();
    user.setEmailId("test@example.com");
    user.setLoginOption(Constants.LOGIN_OPTION_SOCIAL_IDP);
    if (user.getLoginOption() != null && user.getLoginOption().equalsIgnoreCase(Constants.LOGIN_OPTION_SOCIAL_IDP)) {
      String result = cognitoAsyncService.sendWelcomeEmail(user);
      assertEquals("Welcome mail has been re-sent to the Social IDP user successfully.", result);
    }
  }
}
