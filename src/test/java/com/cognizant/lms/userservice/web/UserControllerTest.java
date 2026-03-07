package com.cognizant.lms.userservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UpdateUserRequest;
import com.cognizant.lms.userservice.dto.UserSummaryResponse;
import com.cognizant.lms.userservice.service.CognitoAsyncServiceImpl;
import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.UserNotFoundException;
import com.cognizant.lms.userservice.service.DBImportServiceImpl;
import com.cognizant.lms.userservice.service.UserGlobalSearchHistoryService;
import com.cognizant.lms.userservice.service.UserActivityLogService;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(UserController.class)
@ExtendWith(SpringExtension.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @InjectMocks
  private UserController userController;

  @MockBean
  private UserService userService;

  @MockBean
  private UserActivityLogService userActivityLogService;

  @Autowired
  private WebApplicationContext wac;

  @MockBean
  private DBImportServiceImpl dbImportServiceImpl;

  @MockBean
  private CognitoAsyncServiceImpl cognitoAsyncServiceImpl;

  @MockBean
  private UserGlobalSearchHistoryService userGlobalSearchHistoryService;

  @BeforeEach
  public void setUp() {
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
  }

  private User user;
  private final String endpoint = "/temp-password";

  private static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @ParameterizedTest
  @CsvSource({
          "ADD USERS, 200",
          "DE-ACTIVATE USERS, 200",
          "UPDATE USERS, 200",
          "invalid, 400"
  })
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testUploadUsers_withDifferentActions_returnsExpectedStatus(String action, int expectedStatus)
          throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    MultipartFile file = mock(MultipartFile.class);

    if (Constants.ACTION_ADD.equals(action)) {
      when(userService.uploadUsers(any(), eq(action)))
              .thenReturn(new FileUploadResponse());
    } else if (Constants.ACTION_DEACTIVATE.equals(action)) {
      when(userService.deactivateUser(any(), eq(action)))
              .thenReturn(new FileUploadResponse());
    } else if (Constants.ACTION_UPDATE.equals(action)) {
      when(userService.updateUsers(any(), eq(action)))
              .thenReturn(new FileUploadResponse());
    }

    mockMvc.perform(multipart("/api/v1/users/upload")
                    .file("file", "content".getBytes(StandardCharsets.UTF_8))
                    .param("action", action))
            .andExpect(status().is(expectedStatus));

    if (expectedStatus == HttpStatus.BAD_REQUEST.value()) {
      mockMvc.perform(multipart("/api/v1/users/upload")
                      .file("file", "content".getBytes(StandardCharsets.UTF_8))
                      .param("action", action))
              .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
              .andExpect(jsonPath("$.error").value("Invalid action"));
    } else {
      mockMvc.perform(multipart("/api/v1/users/upload")
                      .file("file", "content".getBytes(StandardCharsets.UTF_8))
                      .param("action", action))
              .andExpect(jsonPath("$.status").value(expectedStatus));
    }
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testGetUserByPk_success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "validPk";
    user = new User();
    user.setPk("pk");
    user.setSk("sk");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setGsiSortFNLN("gsiSortFNLN");
    user.setName("name");
    user.setInstitutionName("Test Institution");
    user.setUserType("External");
    user.setRole("learner");
    user.setStatus("Active");
    user.setUserAccountExpiryDate("12/12/2026");
    user.setEmailId("john.doe@example.com");
    user.setType("user");
    // Set other fields as needed for your test


    when(userService.getUserByPk(partitionKeyValue)).thenReturn(user);

    mockMvc.perform(get("/api/v1/users/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService).getUserByPk(partitionKeyValue);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testGetUserByPk_failure() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "invalidPk";

    when(userService.getUserByPk(partitionKeyValue)).thenReturn(null);

    mockMvc.perform(get("/api/v1/users/{partitionKeyValue}", partitionKeyValue))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()))
            .andExpect(jsonPath("$.error").value("User not found with pk: " + partitionKeyValue));

    // Verify logging of user data
    verify(userService).getUserByPk(partitionKeyValue);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void updateUserByPk_ValidRequest_ReturnsUpdatedUser() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "user123";
    String sortKey = "userSortKey";
    String updatedUser = "User updated successfully with pk: " + partitionKeyValue;
    UpdateUserRequest request =
            new UpdateUserRequest("John", "Doe", "Institution", "10/10/2026", "learner", "External",
                    "Active", "India");

    when(
            userService.updateUserByPk(eq(partitionKeyValue), any(UpdateUserRequest.class))).thenReturn(
            updatedUser);

    mockMvc.perform(put("/api/v1/users/{partitionKeyValue}", partitionKeyValue)
                    .param("sortKey", sortKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(updatedUser))
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateUserByPk(eq(partitionKeyValue),
            any(UpdateUserRequest.class));
  }

  @Test
  @DisplayName("getUserSummary returns bad request for short search value")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  public void getUserSummary_returnsBadRequestForShortSearchValue() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "admin";
    String institutionName = "Cognizant";
    String searchValue = "ab";

    UserSummaryResponse response = new UserSummaryResponse();
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    response.setError("Value must contain at least 3 characters");

    when(userService.getUserSummary(sortKey, order, null, perPage, userRole,
            institutionName, searchValue, null))
            .thenReturn(response);

    mockMvc.perform(get("/api/v1/users")
                    .param("sortKey", sortKey)
                    .param("order", order)
                    .param("perPage", String.valueOf(perPage))
                    .param("role", userRole)
                    .param("institution", institutionName)
                    .param("search", searchValue))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.error").value("Value must contain at least 3 characters"));
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void uploadUsers_withInvalidAction_returnsBadRequest() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    mockMvc.perform(multipart("/api/v1/users/upload")
                    .file("file", "dummy content".getBytes(StandardCharsets.UTF_8))
                    .param("action", "invalidAction"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid action"));
  }

  @Test
  @DisplayName("getUserSummary returns user summary successfully")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  public void getUserSummary_returnsUserSummarySuccessfully() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UserSummaryResponse result = new UserSummaryResponse();
    List<User> users = List.of(
            new User("abc", "xyz", "Cognizant", "abc.xyz@cognizant.com", "Internal", "admin",
                    "12-12-2025", "Y", ""));
    result.setData(users);
    result.setLastEvaluatedKey("");
    result.setCount(1);
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "admin";
    String institutionName = "Cognizant";
    when(userService.getUserSummary(sortKey, order, null, perPage, userRole,
            institutionName, null, null))
            .thenReturn(result);
    mockMvc.perform(get("/api/v1/users")
                    .param("sortKey", sortKey)
                    .param("order", order)
                    .param("perPage", String.valueOf(perPage))
                    .param("role", userRole)
                    .param("institution", institutionName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].firstName").value("abc"))
            .andExpect(jsonPath("$.data[0].lastName").value("xyz"))
            .andExpect(jsonPath("$.data[0].institutionName").value("Cognizant"))
            .andExpect(jsonPath("$.count").value(1));
  }

  @Test
  @DisplayName("getUserSummary returns empty list when no users found")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  public void getUserSummary_returnsEmptyListWhenNoUsersFound() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UserSummaryResponse result = new UserSummaryResponse();
    result.setData(List.of());
    result.setLastEvaluatedKey("");
    result.setCount(0);
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "admin";
    String institutionName = "Cognizant";
    when(userService.getUserSummary(sortKey, order, null, perPage, userRole,
            institutionName, null, null))
            .thenReturn(result);
    mockMvc.perform(get("/api/v1/users")
                    .param("sortKey", sortKey)
                    .param("order", order)
                    .param("perPage", String.valueOf(perPage))
                    .param("role", userRole)
                    .param("institution", institutionName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.count").value(0));
  }

  @Test
  @DisplayName("getUserSummary handles null lastEvaluatedKey")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  public void getUserSummary_handlesNullLastEvaluatedKey() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UserSummaryResponse result = new UserSummaryResponse();
    List<User> users = List.of(
            new User("abc", "xyz", "Cognizant", "abc.xyz@cognizant.com", "Internal", "admin",
                    "12-12-2025", "Y", ""));
    result.setData(users);
    result.setLastEvaluatedKey("");
    result.setCount(1);
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "admin";
    String institutionName = "Cognizant";
    when(userService.getUserSummary(sortKey, order, null, perPage, userRole,
            institutionName, null, null))
            .thenReturn(result);
    mockMvc.perform(get("/api/v1/users")
                    .param("sortKey", sortKey)
                    .param("order", order)
                    .param("perPage", String.valueOf(perPage))
                    .param("role", userRole)
                    .param("institution", institutionName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].firstName").value("abc"))
            .andExpect(jsonPath("$.data[0].lastName").value("xyz"))
            .andExpect(jsonPath("$.data[0].institutionName").value("Cognizant"))
            .andExpect(jsonPath("$.count").value(1));
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void getUserInstitutions_returnsInstitutions() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    List<String> institutions = Arrays.asList("Institution1", "Institution2");
    when(userService.getUserInstitutions(anyString())).thenReturn(institutions);
    mockMvc.perform(get("/api/v1/users/institutions")
                    .param("sortKey", "sortKey"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(institutions.size()))
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testSendWelcomeEmail_Success() throws Exception {
    String email = "test@example.com";
    String responseMessage = "Welcome email sent successfully";

    when(userService.sendWelcomeEmail(email)).thenReturn(responseMessage);

    mockMvc.perform(post("/api/v1/users/email")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(Map.of("email", email))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value(responseMessage))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).sendWelcomeEmail(email);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testSendWelcomeEmail_EmailNotFound() throws Exception {
    String email = "test@example.com";

    when(userService.sendWelcomeEmail(email)).thenReturn(null);

    mockMvc.perform(post("/api/v1/users/email")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(Map.of("email", email))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
            .andExpect(jsonPath("$.error").value("Email not found in Cognito user pool"));

    verify(userService, times(1)).sendWelcomeEmail(email);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testSendWelcomeEmail_InternalServerError() throws Exception {
    String email = "test@example.com";

    when(userService.sendWelcomeEmail(email)).thenThrow(new RuntimeException("Unexpected error"));

    mockMvc.perform(post("/api/v1/users/email")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(Map.of("email", email))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("An error occurred while resending the welcome email"));

    verify(userService, times(1)).sendWelcomeEmail(email);
  }

  @Test
  @DisplayName("scheduleUserExpiry returns success response")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testScheduleUserExpiry_Success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String responseMessage = "Scheduler completed successfully";
    when(userService.scheduleUserExpiry()).thenReturn(responseMessage);
    mockMvc.perform(post("/api/v1/users/scheduleUserExpiry"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(responseMessage))
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.error").isEmpty());
    verify(userService, times(1)).scheduleUserExpiry();
  }

  @Test
  @DisplayName("scheduleUserExpiry handles internal server error")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void testScheduleUserExpiry_InternalServerError() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    when(userService.scheduleUserExpiry()).thenThrow(new RuntimeException("Unexpected error"));
    mockMvc.perform(post("/api/v1/users/scheduleUserExpiry"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(
                    jsonPath("$.error").value("An unexpected error occurred Unexpected error"));
    verify(userService, times(1)).scheduleUserExpiry();
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  @DisplayName("getUserByEmailId returns user when user exists")
  void getUserByEmailId_ReturnsUser_WhenUserExists() throws Exception {
    String emailId = "test@example.com";
    String status = "Active";
    User user = new User();
    user.setEmailId(emailId);
    user.setStatus(status);

    when(userService.getUserByEmailId(emailId, status)).thenReturn(user);

    mockMvc.perform(get("/api/v1/users/email/{email}", emailId)
                    .param("status", status))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data.emailId").value(emailId))
            .andExpect(jsonPath("$.data.status").value(status))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).getUserByEmailId(emailId, status);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  @DisplayName("getUserByEmailId returns not found when user does not exist")
  void getUserByEmailId_ReturnsNotFound_WhenUserDoesNotExist() throws Exception {
    String emailId = "nonexistent@example.com";
    String status = "Active";

    when(userService.getUserByEmailId(emailId, status)).thenReturn(null);

    mockMvc.perform(get("/api/v1/users/email/{email}", emailId)
                    .param("status", status))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()))
            .andExpect(jsonPath("$.error").value("User not found with emailId: " + emailId));

    verify(userService, times(1)).getUserByEmailId(emailId, status);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  @DisplayName("getUserByEmailId returns user when status is not provided")
  void getUserByEmailId_ReturnsUser_WhenStatusIsNotProvided() throws Exception {
    String emailId = "test@example.com";
    User user = new User();
    user.setEmailId(emailId);

    when(userService.getUserByEmailId(emailId, null)).thenReturn(user);

    mockMvc.perform(get("/api/v1/users/email/{email}", emailId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data.emailId").value(emailId))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).getUserByEmailId(emailId, null);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void getValidUsers_returnsValidUsers() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    List<String> userEmails = Arrays.asList("user1@example.com", "user2@example.com");
    List<Map<String, String>> validUsers = List.of(
            Map.of("userId", "1", "email", "user1@example.com"),
            Map.of("userId", "2", "email", "user2@example.com")
    );

    when(userService.validUsers(userEmails)).thenReturn(validUsers);

    mockMvc.perform(get("/api/v1/users/validUsers")
                    .param("userEmails", "user1@example.com", "user2@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(validUsers.size()))
            .andExpect(jsonPath("$.data[0].userId").value("1"))
            .andExpect(jsonPath("$.data[0].email").value("user1@example.com"))
            .andExpect(jsonPath("$.data[1].userId").value("2"))
            .andExpect(jsonPath("$.data[1].email").value("user2@example.com"));

    verify(userService, times(1)).validUsers(userEmails);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void getValidUsers_returnsEmptyListWhenNoValidUsers() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    List<String> userEmails = Arrays.asList("invalid1@example.com", "invalid2@example.com");
    List<Map<String, String>> validUsers = List.of();

    when(userService.validUsers(userEmails)).thenReturn(validUsers);

    mockMvc.perform(get("/api/v1/users/validUsers")
                    .param("userEmails", "invalid1@example.com", "invalid2@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());

    verify(userService, times(1)).validUsers(userEmails);
  }


  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testRecreateUserInCognitoAndSendWelcomeEmail_Success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String email = "test@example.com";
    when(userService.recreateUserInCognitoAndSendWelcomeEmail(email)).thenReturn(true);
    mockMvc.perform(post("/api/v1/users/recreate")
                    .param("email", email))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("User recreated in Cognito and welcome email sent successfully."))
            .andExpect(jsonPath("$.error").isEmpty());
    verify(userService, times(1)).recreateUserInCognitoAndSendWelcomeEmail(email);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testRecreateUserInCognitoAndSendWelcomeEmail_Failure() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String email = "test@example.com";
    when(userService.recreateUserInCognitoAndSendWelcomeEmail(email)).thenReturn(false);
    mockMvc.perform(post("/api/v1/users/recreate")
                    .param("email", email))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.error").value("Failed to recreate user in Cognito or send welcome email."));
    verify(userService, times(1)).recreateUserInCognitoAndSendWelcomeEmail(email);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testRecreateUserInCognitoAndSendWelcomeEmail_Exception() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String email = "test@example.com";
    when(userService.recreateUserInCognitoAndSendWelcomeEmail(email)).thenThrow(new RuntimeException("Unexpected error"));
    mockMvc.perform(post("/api/v1/users/recreate")
                    .param("email", email))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("An unexpected error occurred: Unexpected error"));
    verify(userService, times(1)).recreateUserInCognitoAndSendWelcomeEmail(email);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testMigratePasswordChangedDate_Success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String successMessage = "Migration completed successfully.";
    doNothing().when(userService).migratePasswordChangedDate();

    mockMvc.perform(post("/api/v1/users/migratePasswordChangedDate"))
            .andExpect(status().isOk())
            .andExpect(content().string(successMessage));

    verify(userService, times(1)).migratePasswordChangedDate();
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testMigratePasswordChangedDate_Failure() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String errorMessage = "Migration failed: Unexpected error occurred";
    doThrow(new RuntimeException("Unexpected error occurred")).when(userService).migratePasswordChangedDate();

    mockMvc.perform(post("/api/v1/users/migratePasswordChangedDate"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(errorMessage));

    verify(userService, times(1)).migratePasswordChangedDate();
  }


  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testUpdateDate_Success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UpdateDateDTO updateDateDTO = new UpdateDateDTO();
    updateDateDTO.setEmailId("test@example.com");
    updateDateDTO.setLastLoginTimestamp("2023-10-01T10:00:00Z");
    updateDateDTO.setPasswordChangedDate("2023-10-02T10:00:00Z");

    HttpResponse response = new HttpResponse();
    response.setStatus(HttpStatus.OK.value());
    response.setData("User date fields updated successfully.");

    when(userService.updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO)).thenReturn("User date fields updated successfully.");

    mockMvc.perform(post("/api/v1/users/updateDate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(updateDateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("User date fields updated successfully."))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SUPER_ADMIN"})
  void testUpdateDate_NoFieldsToUpdate() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UpdateDateDTO updateDateDTO = new UpdateDateDTO();
    updateDateDTO.setEmailId("test@example.com");

    when(userService.updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO)).thenReturn("No fields to update.");

    mockMvc.perform(post("/api/v1/users/updateDate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(updateDateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("No fields to update."))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateLastLoginTimeStampAndPasswordChangedDate(updateDateDTO);
  }

  @Test
  @DisplayName("getUserSummary returns user summary successfully with country field")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  public void getUserSummary_returnsUserSummarySuccessfully_WithCountryField() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    UserSummaryResponse result = new UserSummaryResponse();
    List<User> users = List.of(
            User.builder()
                    .firstName("abc")
                    .lastName("xyz")
                    .institutionName("Cognizant")
                    .emailId("abc.xyz@cognizant.com")
                    .userType("Internal")
                    .role("admin")
                    .userAccountExpiryDate("12-12-2025")
                    .status("Y")
                    .country("India")
                    .build()
    );
    result.setData(users);
    result.setLastEvaluatedKey("");
    result.setCount(1);
    String sortKey = "name";
    String order = "asc";
    String perPage = "10";
    String userRole = "admin";
    String institutionName = "Cognizant";
    when(userService.getUserSummary(sortKey, order, null, perPage, userRole,
            institutionName, null, null))
            .thenReturn(result);
    mockMvc.perform(get("/api/v1/users")
                    .param("sortKey", sortKey)
                    .param("order", order)
                    .param("perPage", String.valueOf(perPage))
                    .param("role", userRole)
                    .param("institution", institutionName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].firstName").value("abc"))
            .andExpect(jsonPath("$.data[0].lastName").value("xyz"))
            .andExpect(jsonPath("$.data[0].institutionName").value("Cognizant"))
            .andExpect(jsonPath("$.data[0].country").value("India"))
            .andExpect(jsonPath("$.count").value(1));
  }

  // Test cases for updateTermsAccepted method
  @Test
  @DisplayName("updateTermsAccepted - Success scenario with learner role")
  @WithMockUser(username = "learner", authorities = {"ROLE_LEARNER"})
  void updateTermsAccepted_Success_WithLearnerRole() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "user123";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - Success scenario with content-author role")
  @WithMockUser(username = "author", authorities = {"ROLE_CONTENT_AUTHOR"})
  void updateTermsAccepted_Success_WithContentAuthorRole() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "author456";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - Success scenario with system-admin role")
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void updateTermsAccepted_Success_WithSystemAdminRole() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "admin789";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - Success scenario with super-admin role")
  @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
  void updateTermsAccepted_Success_WithSuperAdminRole() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "superadmin999";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - Internal Server Error when service throws exception")
  @WithMockUser(username = "learner", authorities = {"ROLE_LEARNER"})
  void updateTermsAccepted_InternalServerError() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "user123";
    String errorMessage = "Database connection failed";

    doThrow(new RuntimeException(errorMessage)).when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("Failed to update terms accepted status: " + errorMessage));

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }


  @Test
  @DisplayName("updateTermsAccepted - Success with special characters in partition key")
  @WithMockUser(username = "learner", authorities = {"ROLE_LEARNER"})
  void updateTermsAccepted_Success_WithSpecialCharactersInPartitionKey() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "user@domain.com#123";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - Success with null partition key value")
  @WithMockUser(username = "learner", authorities = {"ROLE_LEARNER"})
  void updateTermsAccepted_Success_WithNullPartitionKey() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "null";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @DisplayName("updateTermsAccepted - UserNotFoundException handled")
  @WithMockUser(username = "learner", authorities = {"ROLE_LEARNER"})
  void updateTermsAccepted_UserNotFoundException() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "nonexistentuser";
    String errorMessage = "User not found";

    doThrow(new UserNotFoundException(errorMessage)).when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("Failed to update terms accepted status: " + errorMessage));

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @ParameterizedTest
  @DisplayName("updateTermsAccepted - Success with different authorized roles")
  @CsvSource({
          "ROLE_LEARNER",
          "ROLE_CONTENT_AUTHOR",
          "ROLE_SYSTEM_ADMIN",
          "ROLE_SUPER_ADMIN"
  })
  void updateTermsAccepted_Success_WithAuthorizedRoles(String role) throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String partitionKeyValue = "user123";

    doNothing().when(userService).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);

    mockMvc.perform(put("/api/v1/users/termsAccepted/{partitionKeyValue}", partitionKeyValue)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles(role.replace("ROLE_", ""))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Terms accepted status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateTermsAcceptedByPartitionKeyValue(partitionKeyValue);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void saveSearchQuery_Success() throws Exception {
    RecentSearchRequest request = new RecentSearchRequest();
    String expectedResponse = "Search saved";
    when(userGlobalSearchHistoryService.saveRecentSearches(any(RecentSearchRequest.class)))
            .thenReturn(expectedResponse);

    mockMvc.perform(post("/api/v1/users/searchQuery")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value(expectedResponse))
            .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void removeRecentSearches_Success() throws Exception {
    String userId = "user123";
    String searchKeyword = "test";

    doNothing().when(userGlobalSearchHistoryService).updateSearchQueryStatus(userId, searchKeyword);

    mockMvc.perform(put("/api/v1/users/searchQuery/remove/{userId}", userId)
                    .param("searchKeyword", searchKeyword)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("Search query removed successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userGlobalSearchHistoryService, times(1)).updateSearchQueryStatus(userId, searchKeyword);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void removeRecentSearches_InternalServerError() throws Exception {
    String userId = "user123";
    String searchKeyword = "test";
    String errorMessage = "DB error";

    doThrow(new RuntimeException(errorMessage)).when(userGlobalSearchHistoryService)
            .updateSearchQueryStatus(userId, searchKeyword);

    mockMvc.perform(put("/api/v1/users/searchQuery/remove/{userId}", userId)
                    .with(csrf())
                    .param("searchKeyword", searchKeyword)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("Failed to remove recent searches: " + errorMessage));

    verify(userGlobalSearchHistoryService, times(1)).updateSearchQueryStatus(userId, searchKeyword);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void getRecentSearches_ReturnsRecentSearches_WhenExists() throws Exception {
    String userId = "user123";
    List<String> searches = Arrays.asList("search1", "search2");
    when(userGlobalSearchHistoryService.getLatestSearches(userId)).thenReturn(searches);

    mockMvc.perform(get("/api/v1/users/search/recentQuery/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data[0]").value("search1"))
            .andExpect(jsonPath("$.data[1]").value("search2"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userGlobalSearchHistoryService, times(1)).getLatestSearches(userId);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void getRecentSearches_ReturnsNoContent_WhenNoSearches() throws Exception {
    String userId = "user123";
    when(userGlobalSearchHistoryService.getLatestSearches(userId)).thenReturn(null);

    mockMvc.perform(get("/api/v1/users/search/recentQuery/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()))
            .andExpect(jsonPath("$.error").value("No recent searches available: " + userId));

    verify(userGlobalSearchHistoryService, times(1)).getLatestSearches(userId);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void updateLearnerPreferredView_Success() throws Exception {
    String userId = "user123";
    String preferredUI = "DARK_MODE";
    String resultMessage = "Preferred UI updated successfully";

    when(userService.updatePreferredUI(userId, preferredUI)).thenReturn(resultMessage);

    mockMvc.perform(put("/api/v1/users/preferred-ui/{userId}", userId)
            .with(csrf())
            .param("preferredUI", preferredUI)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
        .andExpect(jsonPath("$.data").value(resultMessage))
        .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updatePreferredUI(userId, preferredUI);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void updateLearnerPreferredView_InternalServerError() throws Exception {
    String userId = "user123";
    String preferredUI = "DARK_MODE";
    String errorMessage = "Update failed";

    when(userService.updatePreferredUI(userId, preferredUI)).thenThrow(new RuntimeException(errorMessage));

    mockMvc.perform(put("/api/v1/users/preferred-ui/{userId}", userId)
            .with(csrf())
            .param("preferredUI", preferredUI)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error").value(errorMessage));

    verify(userService, times(1)).updatePreferredUI(userId, preferredUI);
  }

  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void updateIsWatchedTutorial_success() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String userId = "user123";
    // Do not throw exception for success case
    doNothing().when(userService).updateIsWatchedTutorial(userId);

    mockMvc.perform(put("/api/v1/users/tutorial-preferences/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").value("tutorial watched status updated successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).updateIsWatchedTutorial(userId);
  }


  @Test
  @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
  void recordLaunch_success() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        String userId = "user123";
        doNothing().when(userService).recordVideoLaunch(userId);

        mockMvc.perform(post("/api/v1/users/launch/{userId}", userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data").value("stored number of times  video launched"))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(userService, times(1)).recordVideoLaunch(userId);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_SYSTEM_ADMIN"})
    void recordLaunch_failure() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        String userId = "user123";
        doThrow(new RuntimeException("Some error")).when(userService).recordVideoLaunch(userId);

        mockMvc.perform(post("/api/v1/users/launch/{userId}", userId)
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Failed to number of times  video launched: Some error")));

        verify(userService, times(1)).recordVideoLaunch(userId);
    }

  @Test
  @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
  void addAdminUser_Success() throws Exception {
    String tenantId = "tenant-1";
    String type = "superadmin";
    User user = new User();
    user.setEmailId("admin@domain.com");

    when(userService.addAdminUser(any(User.class), eq(type), eq(tenantId))).thenReturn(true);

    mockMvc.perform(post("/api/v1/users/tenants/{tenantId}/admins", tenantId)
                    .param("type", type)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(user))
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
            .andExpect(jsonPath("$.data").value("User created successfully"))
            .andExpect(jsonPath("$.error").isEmpty());

    verify(userService, times(1)).addAdminUser(any(User.class), eq(type), eq(tenantId));
  }

  @Test
  @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
  void addAdminUser_Failure() throws Exception {
    String tenantId = "tenant-1";
    String type = "tenantadmin";
    User user = new User();
    user.setEmailId("admin@domain.com");

    when(userService.addAdminUser(any(User.class), eq(type), eq(tenantId))).thenReturn(false);

    mockMvc.perform(post("/api/v1/users/tenants/{tenantId}/admins", tenantId)
                    .param("type", type)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(user))
                    .with(csrf()))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("Failed to create user"));

    verify(userService, times(1)).addAdminUser(any(User.class), eq(type), eq(tenantId));
  }

  @Test
  @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
  void addAdminUser_Exception() throws Exception {
    String tenantId = "tenant-1";
    String type = "tenantadmin";
    User user = new User();
    user.setEmailId("admin@domain.com");

    when(userService.addAdminUser(any(User.class), eq(type), eq(tenantId)))
            .thenThrow(new RuntimeException("Unexpected error"));

    mockMvc.perform(post("/api/v1/users/tenants/{tenantId}/admins", tenantId)
                    .param("type", type)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(user))
                    .with(csrf()))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(jsonPath("$.error").value("Unexpected error"));

    verify(userService, times(1)).addAdminUser(any(User.class), eq(type), eq(tenantId));
  }
}
