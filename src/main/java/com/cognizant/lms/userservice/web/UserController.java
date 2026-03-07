package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.LoggedInUser;
import com.cognizant.lms.userservice.dto.RecentSearchRequest;
import com.cognizant.lms.userservice.dto.UpdateUserRequest;
import com.cognizant.lms.userservice.dto.UserSettingsRequest;
import com.cognizant.lms.userservice.dto.UserSummaryResponse;
import com.cognizant.lms.userservice.dto.UpdateDateDTO;
import com.cognizant.lms.userservice.dto.UserEmailDto;
import com.cognizant.lms.userservice.service.DBImportService;
import com.cognizant.lms.userservice.service.UserGlobalSearchHistoryService;
import com.cognizant.lms.userservice.service.UserActivityLogService;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.LogUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import com.cognizant.lms.userservice.utils.SanitizeUtil;
import org.springframework.beans.factory.annotation.Value;
import com.cognizant.lms.userservice.utils.TenantUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/users")
@Slf4j
public class UserController {
  @Autowired
  private UserService userService;

  @Autowired
  private UserActivityLogService userActivityLogService;

  @Autowired
  private DBImportService dbImportService;

  @Autowired
  private UserGlobalSearchHistoryService userGlobalSearchHistoryService;

  @PostMapping("/upload")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> uploadUsers(
      @RequestParam("file") MultipartFile file,
      @RequestParam("action") String action) throws Exception {
    if (action.equalsIgnoreCase(Constants.ACTION_ADD)) {
      FileUploadResponse fileUploadResponse =
          userService.uploadUsers(file, action);
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.OK.value());
      response.setData(fileUploadResponse);
      response.setError(null);
      return ResponseEntity.ok(response);
    } else if (action.equalsIgnoreCase(Constants.ACTION_DEACTIVATE)) {
      FileUploadResponse fileUploadResponse =
          userService.deactivateUser(file, action);
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.OK.value());
      response.setData(fileUploadResponse);
      response.setError(null);
      return ResponseEntity.ok(response);
    } else if (action.equalsIgnoreCase(Constants.ACTION_UPDATE)) {
      FileUploadResponse fileUploadResponse =
          userService.updateUsers(file, action);
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.OK.value());
      response.setData(fileUploadResponse);
      response.setError(null);
      return ResponseEntity.ok(response);
    } else if(action.equalsIgnoreCase(Constants.ACTION_REACTIVATE)){
      FileUploadResponse fileUploadResponse =
          userService.reActivateUsers(file, action);
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.OK.value());
      response.setData(fileUploadResponse);
      response.setError(null);
      return ResponseEntity.ok(response);
    }
    else {
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("Invalid action");
      return ResponseEntity.badRequest().body(response);
    }
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner-delivery-admin','async-facilitator')")
  public ResponseEntity<UserSummaryResponse> getUserSummary(
      @RequestParam(value = "sortKey", defaultValue = "${AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY}")
      String sortKey,
      @RequestParam(value = "order", defaultValue = "${AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER}")
      String order,
      @RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKeyEncoded,
      @RequestParam(value = "perPage", defaultValue = "${DEFAULT_ROWS_PER_PAGE}") String perPage,
      @RequestParam(value = "role", required = false) String userRole,
      @RequestParam(value = "institution", required = false) String institutionName,
      @RequestParam(value = "search", required = false) String searchValue,
      @RequestParam(value = "status", required = false) String status) {

      UserSummaryResponse response =
              userService.getUserSummary(sortKey, order, lastEvaluatedKeyEncoded, perPage, userRole,
                      institutionName, searchValue, status);
      if (response.getStatus() == HttpStatus.BAD_REQUEST.value()) {
        return ResponseEntity.badRequest().body(response);
      }
      return ResponseEntity.ok(response);
  }

  @GetMapping("/download")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<Resource> downloadFile(@RequestParam("fileName") String filename,
                                               @RequestParam("fileType") String fileType) {
    try {
      // Normalize the filename to prevent path traversal attacks
      String sanitizedFilename = FilenameUtils.getName(filename);
      Path filePath = Paths.get(sanitizedFilename).normalize();
      if (filePath.startsWith("..") || filePath.isAbsolute()) {
        log.error("Invalid file name: {}", filename);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
      }
      Resource fileResource = userService.getDownloadErrorLogFile(filePath.toString(), fileType);
      return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, Constants.ATTACHMENT_FILENAME
                      + Constants.SLASH + filePath.getFileName().toString() + Constants.SLASH)
              .contentType(MediaType.TEXT_PLAIN)
              .body(fileResource);
    } catch (FileNotFoundException e) {
      log.error("File not found: {}", filename, e);
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
              .body(null);
    } catch (IOException e) {
      log.error("Error reading file: {}", filename, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(null);
    } catch (Exception e) {
      log.error("Unexpected error: {}", filename, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(null);
    }
  }


  @GetMapping("/institutions")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author')")
  public ResponseEntity<HttpResponse> getUserInstitutions(
      @RequestParam(value = "sortKey", defaultValue = "${AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY}")
      String sortKey) {
    HttpResponse response = new HttpResponse();
    List<String> institutions = userService.getUserInstitutions(sortKey);
    response.setData(institutions);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    log.info("Fetching all institutions {}", response.getData());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{partitionKeyValue}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author', 'learner')")
  public ResponseEntity<HttpResponse> getUserByPk(@PathVariable String partitionKeyValue) {
    HttpResponse response = new HttpResponse();
    User user = userService.getUserByPk(partitionKeyValue);
    if (user == null) {
      response.setStatus(HttpStatus.NO_CONTENT.value());
      response.setError("User not found with pk: " + partitionKeyValue);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } else {
      response.setData(user);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      log.info("Fetched user by pk {}", partitionKeyValue);
      return ResponseEntity.ok(response);
    }

  }

  @PutMapping("/{partitionKeyValue}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> updateUserByPk(@PathVariable String partitionKeyValue,
                                                     @RequestBody
                                                     UpdateUserRequest updateUserRequest) {
    HttpResponse response = new HttpResponse();
    String updatedUser = userService.updateUserByPk(partitionKeyValue, updateUserRequest);
    response.setData(updatedUser);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    log.info("Updating user by pk {}", response.getData());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/email")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> sendWelcomeEmail(@RequestBody Map<String, String> requestBody) {
    HttpResponse response = new HttpResponse();
    try {
      String email = requestBody.get("email");
      String responseMessage = userService.sendWelcomeEmail(email);
      log.info("Resend welcome email response: {}", responseMessage);
      if (responseMessage != null) {
        response.setData(responseMessage);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
      } else {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setError("Email not found in Cognito user pool");
      }
    } catch (Exception e) {
      log.error("An error occurred while resending the welcome email : " + e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An error occurred while resending the welcome email");
    }
    return ResponseEntity.status(response.getStatus()).body(response);
  }

  @PostMapping("/scheduleUserExpiry")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> scheduleUserExpiry() {
    HttpResponse response = new HttpResponse();
    log.info("Scheduler started for user expiration");
    String responseMessage = userService.scheduleUserExpiry();
    log.info("Scheduler completed for user expiration with response : {}", responseMessage);
    response.setData(responseMessage);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    return ResponseEntity.status(response.getStatus()).body(response);
  }

  @PostMapping("/current-user/register-login")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','learner','mentor','content-author',"
      + "'catalog-admin')")
  public ResponseEntity<HttpResponse> registerLoggedInUser(HttpServletRequest request,
                                                           @RequestParam(value = "portal",
                                                                   required = false) String portal) {
    String deviceDetails = request.getHeader("User-Agent");
    String ipAddress = request.getRemoteAddr();
    log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
        ProcessConstants.IN_PROGRESS) + "Fetching logged in user");
    HttpResponse response = new HttpResponse();
    LoggedInUser loggedInUser = userService.registerLoggedInUser(deviceDetails, ipAddress, portal);

    if(loggedInUser == null){
        response.setData(null);
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setError("Portal value does not match");
        log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
            ProcessConstants.FAILED) + "Portal value does not match");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    response.setData(loggedInUser);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
            ProcessConstants.COMPLETED) + "Fetched logged in user with userId {} and roles {}",
        loggedInUser.getUserId(), loggedInUser.getUserRoles());
    return ResponseEntity.ok().body(response);
  }


  @GetMapping("/email/{email}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author', 'learner', 'mentor')")
  public ResponseEntity<HttpResponse> getUserByEmailId(@PathVariable(name = "email") String email,
                                                       @RequestParam(value = "status",
                                                           required = false) String status) {
    HttpResponse response = new HttpResponse();
    User user = userService.getUserByEmailId(email, status);
    if (user == null) {
      response.setStatus(HttpStatus.NO_CONTENT.value());
      response.setError("User not found with emailId: " + email);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } else {
      user.setTenant(TenantUtil.getTenantDetails());
      response.setData(user);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      log.info("Fetching user {} by emailId", user.getPk());
      return ResponseEntity.ok(response);
    }
  }

  @GetMapping("/validUsers")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> getValidUsers(
          @RequestParam("userEmails") List<String> userEmails) {
    log.info("req received");
    List<Map<String, String>> validaUsers= userService.validUsers(userEmails);
    HttpResponse response = new HttpResponse();
    response.setData(validaUsers);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    return ResponseEntity.status(response.getStatus()).body(response);
  }
  @PostMapping("/recreate")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> recreateUserInCognitoAndSendWelcomeEmail(@RequestParam String email) {
    HttpResponse response = new HttpResponse();
    try {
      boolean isSuccess = userService.recreateUserInCognitoAndSendWelcomeEmail(email);
      if (isSuccess) {
        response.setData("User recreated in Cognito and welcome email sent successfully.");
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        return ResponseEntity.ok(response);
      } else {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Failed to recreate user in Cognito or send welcome email.");
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      log.error("Error recreating user in Cognito and sending welcome email: {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An unexpected error occurred: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/temp-password")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author', 'learner', 'mentor')")
  public ResponseEntity<HttpResponse> generateAndSendTempPassword(@RequestParam String emailId) {
    HttpResponse response = new HttpResponse();
    try {
      boolean isSuccess = userService.setPasswordAndSendEmailAsync(emailId);

      if (isSuccess) {
        response.setData("Password is set in Cognito and temporary password mail is sent to: " + emailId);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        return ResponseEntity.ok(response);
      } else {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Failed to set password in Cognito and send temporary password mail.");
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      log.error("Error in setting Password in Cognito and temporary password is sent to {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An unexpected error occurred: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  //for temporary use , needs to be removed later

  @PostMapping("/updateDate")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> updateDate(@RequestBody UpdateDateDTO updateDateDTO) {
    HttpResponse response = new HttpResponse();
    try {
      String result = userService.updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO);
      response.setData(result);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/migratePasswordChangedDate")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<String> migratePasswordChangedDate() {
    try {
      log.info("Triggering migration for passwordChangedDate.");
      userService.migratePasswordChangedDate();
      return ResponseEntity.ok("Migration completed successfully.");
    } catch (Exception e) {
      log.error("Error during migration: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Migration failed: " + e.getMessage());
    }
  }

  @GetMapping("/enableUserInCognito")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> enableUser(@RequestParam String email) {
    log.info("Received request to enable user: {}", email);
    HttpResponse response = new HttpResponse();
    try {
      boolean isSuccess = userService.enableCognitoUserByEmail(email);
      log.info("Enable Cognito user by email result: {}", isSuccess);
      if (isSuccess) {
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
      } else {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Failed to enable user or user not found.");
      }
    } catch (Exception e) {
      log.error("Error enabling user: {}", e.getMessage(), e);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An unexpected error occurred: " + e.getMessage());
    }
    log.info("Enable user response: {}", response);
    return ResponseEntity.status(response.getStatus()).body(response);
  }

  @GetMapping("/disableUserInCognito")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> disableUser(@RequestParam String email) {
    log.info("Received request to disable user: {}", email);
    HttpResponse response = new HttpResponse();
    try {
      boolean isSuccess = userService.disableCognitoUserByEmail(email);
      log.info("Disable Cognito user by email result: {}", isSuccess);
      if (isSuccess) {
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
      } else {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Failed to disable user or user not found.");
      }
    } catch (Exception e) {
      log.error("Error disabling user: {}", e.getMessage(), e);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An unexpected error occurred: " + e.getMessage());
    }
    log.info("Disable user response: {}", response);
    return ResponseEntity.status(response.getStatus()).body(response);
  }

  @PutMapping("/termsAccepted/{partitionKeyValue}")
  @PreAuthorize("hasAnyRole('learner','content-author','system-admin','super-admin')")
  public ResponseEntity<HttpResponse> updateTermsAccepted( @PathVariable String partitionKeyValue) {
    HttpResponse response = new HttpResponse();
    try {
      userService.updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
      response.setStatus(HttpStatus.OK.value());
      response.setData("Terms accepted status updated successfully");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating terms accepted status for user {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to update terms accepted status: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PutMapping("/termsAccepted/migration")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> updateTermsAcceptedMigration() {
    HttpResponse response = new HttpResponse();
    try {
      userService.updateTermsAccepted();
      response.setStatus(HttpStatus.OK.value());
      response.setData("Terms accepted status updated successfully ");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating terms accepted status for user {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to update terms accepted status: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping(path = "/uploadUser",consumes = {"multipart/form-data"})
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> bulkUserUploadUsers(
      @RequestPart("file") MultipartFile file) throws Exception {
    FileUploadResponse fileUploadResponse =
        userService.uploadBulkUsers(file, Constants.ACTION_ADD);
    HttpResponse response = new HttpResponse();
    response.setStatus(HttpStatus.OK.value());
    response.setData(fileUploadResponse);
    response.setError(null);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/userEmails")
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity <HttpResponse> getUserFromUsername (
      @RequestBody  List<String> usernames){
    HttpResponse response = new HttpResponse();
    try {
      response.setData(userService.getUserByUsername(usernames));
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);

    }catch(Exception e){
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping(path = "/deleteBulkUser",consumes = {"multipart/form-data"})
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<HttpResponse> bulkUserUploadDelete(
      @RequestPart("file") MultipartFile file,
      @RequestParam(value = "start", required = false, defaultValue = "1") int start,
      @RequestParam(value = "end", required = false, defaultValue = "100") int end) {
    String fileUploadResponse =
        userService.deleteBulkUsers(file, start, end);
    HttpResponse response = new HttpResponse();
    response.setStatus(HttpStatus.OK.value());
    response.setData(fileUploadResponse);
    response.setError(null);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/searchQuery")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity <HttpResponse> saveSearchQuery (
      @RequestBody RecentSearchRequest recentSearchRequest){
    HttpResponse response = new HttpResponse();
    try {
      response.setData(userGlobalSearchHistoryService.saveRecentSearches(recentSearchRequest));
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);

    }catch(Exception e){
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PutMapping("/searchQuery/remove/{userId}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity<HttpResponse> removeRecentSearches(
      @PathVariable("userId") String userId,
      @RequestParam String searchKeyword
  ) {
    HttpResponse response = new HttpResponse();
    try {
      userGlobalSearchHistoryService.updateSearchQueryStatus(userId,searchKeyword);
      response.setStatus(HttpStatus.OK.value());
      response.setData("Search query removed successfully");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error remove recent searches for user {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to remove recent searches: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @GetMapping("/search/recentQuery/{userId}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity<HttpResponse> getRecentSearches(@PathVariable String userId) {
    HttpResponse response = new HttpResponse();
    List<String> latestSearches = userGlobalSearchHistoryService.getLatestSearches(userId);
    if (latestSearches == null) {
      response.setStatus(HttpStatus.NO_CONTENT.value());
      response.setError("No recent searches available: " + userId);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } else {
      response.setData(latestSearches);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);
    }
  }

  @PostMapping("/activity/logout")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','learner','mentor','content-author',"
          + "'catalog-admin')")
  public ResponseEntity<HttpResponse> saveLogoutActivity(HttpServletRequest request) {
    HttpResponse response = new HttpResponse();
    try {
      String deviceDetails = request.getHeader("User-Agent");
      String ipAddress = request.getRemoteAddr();
      userActivityLogService.saveUserActivityLog(deviceDetails, ipAddress, Constants.USER_ACTIVITY_LOGOUT_TYPE);
      response.setStatus(HttpStatus.OK.value());
      response.setData("Logout activity saved successfully");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error saving logout activity {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to save logout activity: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PutMapping("/update/userSettings/{userId}")
  @PreAuthorize("hasAnyRole('learner','content-author','system-admin','super-admin')")
    public ResponseEntity<HttpResponse> updateUserSettings(@PathVariable String userId,
                                                           @RequestBody UserSettingsRequest request) {
    HttpResponse response = new HttpResponse();
    try {
      userService.updateUserSettings(userId, request.getType(), request.getOption());
      response.setStatus(HttpStatus.OK.value());
      response.setData("User settings updated successfully");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating user settings for user {}: {}", userId, e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to update user settings: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PutMapping("/preferred-ui/{userId}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','learner','mentor','content-author',"
      + "'catalog-admin')")
  public ResponseEntity<HttpResponse> updateLearnerPreferredView(
      @PathVariable("userId") String userId,
      @RequestParam(value = "preferredUI", required = true) String preferredUI) {
    HttpResponse response = new HttpResponse();
    try {
      String result = userService.updatePreferredUI(userId, preferredUI);
      response.setData(result);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      log.info("Successfully updated preferredUI to {} for learner: {}", preferredUI, userId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating preferredUI for userId {}: {}", userId, e.getMessage());
      response.setData(null);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @GetMapping("/emails/list")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity<HttpResponse> listUserEmail() {
    HttpResponse response = new HttpResponse();
    try {
      List<UserEmailDto> result = userService.listUserEmailIdAndUserId();
      response.setData(result);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      log.info("Successfully fetched user emails");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching user emails: {}", e.getMessage());
      response.setData(null);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PutMapping("/tutorial-preferences/{userId}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity<HttpResponse> updateIsWatchedTutorial( @PathVariable String userId) {
    HttpResponse response = new HttpResponse();
    try {
      userService.updateIsWatchedTutorial(userId);
      response.setStatus(HttpStatus.OK.value());
      response.setData("tutorial watched status updated successfully");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating tutorial watched status for user {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to update tutorial watched status: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/launch/{userId}")
  @PreAuthorize("hasAnyRole('system-admin','super-admin', 'learner', 'mentor', 'content-author','learner-delivery-admin','async-facilitator', 'catalog-admin')")
  public ResponseEntity<HttpResponse> recordLaunch(@PathVariable String userId) {
    HttpResponse response = new HttpResponse();
    try {
      userService.recordVideoLaunch(userId);
      response.setStatus(HttpStatus.OK.value());
      response.setData("stored number of times  video launched");
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating number of times  video launched {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Failed to number of times  video launched: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/tenants/{tenantId}/admins")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> addAdminUser(@PathVariable String tenantId,
                                                 @RequestBody User user,
                                                 @RequestParam String type) {
    HttpResponse response = new HttpResponse();
    if (!"superadmin".equalsIgnoreCase(type) && !"tenantadmin".equalsIgnoreCase(type)) {
        response.setData(null);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setError("Only superadmin or tenantadmin types are allowed");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }else{
        try {
            boolean userResult = userService.addAdminUser(user, type, tenantId);
            if (userResult) {
                response.setData("User created successfully");
                response.setStatus(HttpStatus.CREATED.value());
                response.setError(null);
                log.info("Successfully created user");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.setData(null);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setError("Failed to create user");
                log.error("Failed to create user");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
  }
}
