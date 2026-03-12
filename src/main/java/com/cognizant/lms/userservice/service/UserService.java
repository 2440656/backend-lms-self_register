package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.*;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

public interface UserService {


  UserSummaryResponse getUserSummary(String sortKey, String order, String lastEvaluatedKeyEncoded,
                                     String perPage, String userRole,
                                     String institutionName, String searchValue, String status);

  FileUploadResponse reActivateUsers(MultipartFile file, String action) throws Exception;

  FileUploadResponse uploadUsers(MultipartFile file, String action) throws Exception;

  TenantDTO getTenantDetails(String tenantIdentifier);

  User createUserForCognizantSSO(String firstName, String lastName, String email);

  Resource getDownloadErrorLogFile(String filename, String fileType) throws Exception;

  List<String> getUserInstitutions(String gsi);

  FileUploadResponse deactivateUser(MultipartFile file, String action) throws Exception;

  FileUploadResponse updateUsers(MultipartFile file, String action) throws Exception;

  User getUserByPk(String partitionKeyValue);

  List<User> getUserByTenantCode(String partitionKeyValue);

  /**
   * Returns the active user count per tenant code.
   * <p>
   * Keys in the returned map must match the input tenant codes.
   *
   * @param tenantCodes list of tenant codes
   * @return map of tenantCode -> userCount
   */
  Map<String, Integer> getUserCountByTenantCodes(List<String> tenantCodes);

  String updateUserByPk(String partitionKeyValue, UpdateUserRequest updateUserRequest);

  String sendWelcomeEmail(String email) throws Exception;

  EmailSelfRegistrationResponse registerUserByEmail(EmailSelfRegistrationRequest request);

  EmailOtpActionResponse verifyRegistrationOtp(EmailOtpVerificationRequest request);

  EmailOtpActionResponse resendRegistrationOtp(EmailOtpResendRequest request);

  String scheduleUserExpiry();

  LoggedInUser registerLoggedInUser(String deviceDetails, String ipAddress, String portal);

  User getUserByEmailId(String emailId, String status);

  /**
   * Method to validate users based on provided email ids and return the validated users
   *
   * @param userEmails
   * @return
   */
  List<Map<String, String>> validUsers(List<String> userEmails);

  boolean recreateUserInCognitoAndSendWelcomeEmail(String email) throws Exception;

  void migratePasswordChangedDate();

  String updateLastLoginTimeStampAndPasswordChangedDate(UpdateDateDTO updateDateDTO);
  boolean setPasswordAndSendEmailAsync(String emailId);
  boolean enableCognitoUserByEmail(String email) throws Exception;
  boolean disableCognitoUserByEmail(String email) throws Exception;

  void updateTermsAccepted();

  void  updateTermsAcceptedByPartitionKeyValue(String partitionKeyValue);

  FileUploadResponse uploadBulkUsers(MultipartFile file, String action) throws Exception;

  Map<String,String > getUserByUsername (List<String> usernames);

  String deleteBulkUsers(MultipartFile file, int start, int end);

  void updateUserSettings(String userId, String type, String option);

  String updatePreferredUI(String userId, String preferredUI);

  List<UserEmailDto> listUserEmailIdAndUserId();

  void  updateIsWatchedTutorial(String userId);

  void recordVideoLaunch(String userId);

  boolean addAdminUser(User user,String type, String tenantCode)throws Exception;

  void updateUserPersonalDetails(String pk, String sk, String firstName, String lastName, String country, String institutionName, String currentRole);

  String uploadProfilePhoto(String pk, String sk, MultipartFile file) throws Exception;

  void deleteProfilePhoto(String pk, String sk, String photoUrl) throws Exception;

  String getPresignedProfilePhotoUrl(String photoUrl) throws Exception;
}
