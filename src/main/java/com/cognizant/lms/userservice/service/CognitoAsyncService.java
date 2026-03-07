package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.domain.User;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;

import java.util.concurrent.CompletableFuture;

public interface CognitoAsyncService {
  /**
   * Creates a single user asynchronously.
   *
   * @param user User to be created.
   * @return CompletableFuture that completes when the user is created.
   */
  CompletableFuture<Void> createCognitoUserAsync(User user);

  CompletableFuture<Void> sendWelcomeMailToSocialIDP(User user);
  void sendRoleAssignmentEmail(User user, String roleName);

  /**
   * Deletes a single user asynchronously.
   *
   * @param user User to be created.
   * @return CompletableFuture that completes when the user is deleted.
   */
  CompletableFuture<Void> deleteCognitoUserAsync(User user);

  CompletableFuture<Void> deleteBulkCognitoUserAsync(User user);

  CompletableFuture<Void> updateCognitoUserAsync(User user);

  String sendWelcomeEmail(User user) throws Exception;

  CompletableFuture<Void> enableCognitoUserAsync(User user);

  String temporaryPasswordGenerator();
  CompletableFuture<Void> disableCognitoUserAsync(User user);
  void sendTemporaryPasswordEmail(User user, String tempPassword);
  CompletableFuture<Void> setCognitoUserPasswordAsync(String userName, String password, User user);
  CompletableFuture<Void> enableCognitoUserAsyncWithEMail(String emailId);
  CompletableFuture<Void> disableCognitoUserAsyncWithEmail(String emailId);
  AdminGetUserResponse getUserFromCognito(String email);
  void sendReactivateSkillSpringUserEmail(User user, String tempPassword);
  void sendReactivateUserEmail(User user);

  CompletableFuture<Void> recreateCognitoUserAsync(User existingUser, String tempPassword);
  CompletableFuture<Void> createUserWithPoolIdAsync(User user, String userPoolId);
  CompletableFuture<Void> signUpUserAsync(String email, String password, String firstName,
                                          String lastName, String appClientId,
                                          String appClientSecret);
  CompletableFuture<Void> confirmSignUpAsync(String email, String otpCode, String appClientId,
                                             String appClientSecret);
  CompletableFuture<Void> resendSignUpOtpAsync(String email, String appClientId,
                                               String appClientSecret);
}
