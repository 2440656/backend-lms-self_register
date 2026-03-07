package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.CSVProcessResponse;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.dto.TenantDTO;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CSVProcessorTest {

  @Mock
  private CSVValidator csvValidator;

  @Mock
  private LookupDao lookupDao;

  @InjectMocks
  private CSVProcessor csvProcessor;

  @Mock
  private UserFilterSortDao userFilterSortDao;

  @BeforeEach
  void setUp() {
    TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
    MockitoAnnotations.openMocks(this);

    // Mock the behavior of countryDao
    when(lookupDao.getLookupData("Country", null)).thenReturn(List.of(
            new LookupDto("India"),
            new LookupDto("USA"),
            new LookupDto("Canada")
    ));


    csvProcessor = new CSVProcessor(csvValidator);
    // Inject userFilterSortDao via reflection (since no setter)
    // OR add a setter in CSVProcessor: setUserFilterSortDao(UserFilterSortDao dao)
    try {
      var field = CSVProcessor.class.getDeclaredField("userFilterSortDao");
      field.setAccessible(true);
      field.set(csvProcessor, userFilterSortDao);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject userFilterSortDao", e);
    }

    // Inject the mock into CSVProcessor
    csvProcessor.setCountryDao(lookupDao); // Ensure countryDao is set
  }

  @Test
  void processAddUserFile_Success() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,UserType,Role,AccountExpiryDate,ViewOnlyAssignedCourses,LoginOption,Country\n" +
        "John,Doe,Institution,john.doe@example.com,Student,Role,2023-12-31, Y,SSO,India";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateAddUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processAddUserFile(file);

    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertTrue(response.getErrors().isEmpty());
  }

  @Test
  void processAddUserFile_InvalidHeader() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "Invalid Header\nJohn";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(false);

    CSVProcessResponse response = csvProcessor.processAddUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processAddUserFile_Exception() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new RuntimeException("Error"));

    CSVProcessResponse response = csvProcessor.processAddUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processDeActiveUserFile_Success() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "EmailID\njohn.doe@example.com";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_DEACTIVATE_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateDeactivateUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processDeActiveUserFile(file);

    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertTrue(response.getErrors().isEmpty());
  }

  @Test
  void processDeActiveUserFile_InvalidHeader() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "Invalid Header\njohn.doe@example.com";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_DEACTIVATE_USERS_HEADERS))).thenReturn(false);

    CSVProcessResponse response = csvProcessor.processDeActiveUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processDeActiveUserFile_Exception() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new RuntimeException("Error"));

    CSVProcessResponse response = csvProcessor.processDeActiveUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processUpdateUserFile_Success() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,AccountExpiryDate,Country\n" +
        "John,Doe,Institution,john.doe@example.com,2023-12-31,India";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_UPDATE_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateUpdateUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processUpdateUserFile(file);

    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertTrue(response.getErrors().isEmpty());
  }

  @Test
  void processUpdateUserFile_InvalidHeader() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "Invalid Header\nJohn";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_UPDATE_USERS_HEADERS))).thenReturn(false);

    CSVProcessResponse response = csvProcessor.processUpdateUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processUpdateUserFile_Exception() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new RuntimeException("Error"));

    CSVProcessResponse response = csvProcessor.processUpdateUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertEquals(0, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
  }

  @Test
  void processAddUserFile_InvalidCountry() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,UserType,Role,AccountExpiryDate,ViewOnlyAssignedCourses,LoginOption,Country\n" +
            "John,Doe,Institution,john.doe@example.com,Student,Role,2023-12-31,Y,SSO,InvalidCountry";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateAddUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processAddUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
    assertTrue(response.getErrors().get(0).contains("Invalid Country"));
  }

  @Test
  void processUpdateUserFile_InvalidCountry() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,AccountExpiryDate,Country\n" +
            "John,Doe,Institution,john.doe@example.com,2023-12-31,InvalidCountry";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_UPDATE_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateUpdateUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processUpdateUserFile(file);

    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertFalse(response.getErrors().isEmpty());
    assertTrue(response.getErrors().get(0).contains("Invalid Country"));
  }

  @Test
  void processAddUserFile_ValidCountry() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,UserType,Role,AccountExpiryDate,ViewOnlyAssignedCourses,LoginOption,Country\n" +
            "John,Doe,Institution,john.doe@example.com,Student,Role,2023-12-31,Y,SSO,India";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_ADD_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateAddUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processAddUserFile(file);

    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertTrue(response.getErrors().isEmpty());
  }

  @Test
  void processUpdateUserFile_ValidCountry() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "FirstName,LastName,InstitutionName,EmailID,AccountExpiryDate,Country\n" +
            "John,Doe,Institution,john.doe@example.com,2023-12-31,India";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_UPDATE_USERS_HEADERS))).thenReturn(true);
    when(csvValidator.validateUpdateUserFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

    CSVProcessResponse response = csvProcessor.processUpdateUserFile(file);

    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    assertEquals(1, response.getTotalCount());
    assertTrue(response.getErrors().isEmpty());
  }

    @Test
    void processReActivateUsers_Success() throws Exception {
      String csvContent = "EmailID,ExpiryDate\njohn.doe@example.com,2025-12-31";
      MultipartFile file = mockCSVFile(csvContent);

      when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_REACTIVATE_USER))).thenReturn(true);
      when(csvValidator.validateReActivateUser(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

      User user = new User();
      user.setEmailId("john.doe@example.com");
      user.setStatus(Constants.IN_ACTIVE_STATUS);
      when(userFilterSortDao.getUserByEmailId(eq("john.doe@example.com"), eq(Constants.IN_ACTIVE_STATUS))).thenReturn(user);

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(1, response.getSuccessCount());
      assertEquals(0, response.getFailureCount());
      assertEquals(1, response.getTotalCount());
      assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void processReActivateUsers_InvalidHeader() throws Exception {
      String csvContent = "WrongHeader,ExpiryDate\njohn.doe@example.com,2025-12-31";
      MultipartFile file = mockCSVFile(csvContent);

      when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_REACTIVATE_USER))).thenReturn(false);

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(0, response.getSuccessCount());
      assertEquals(0, response.getTotalCount());
      assertEquals(1, response.getErrors().size());
    }

    @Test
    void processReActivateUsers_ExceedsMaxRecords() throws Exception {
      StringBuilder csvBuilder = new StringBuilder("EmailID,ExpiryDate\n");
      for (int i = 0; i < 51; i++) {
        csvBuilder.append("user").append(i).append("@example.com,2025-12-31\n");
      }
      MultipartFile file = mockCSVFile(csvBuilder.toString());

      when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_REACTIVATE_USER))).thenReturn(true);
      when(csvValidator.validateReActivateUser(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());
      when(userFilterSortDao.getUserByEmailId(anyString(), eq(Constants.IN_ACTIVE_STATUS))).thenReturn(new User());

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(50, response.getSuccessCount());
      assertEquals(1, response.getFailureCount());
      assertEquals(51, response.getTotalCount());
      assertFalse(response.getErrors().isEmpty());
    }

    @Test
    void processReActivateUsers_UserNotFound() throws Exception {
      String csvContent = "EmailID,ExpiryDate\nnotfound@example.com,2025-12-31";
      MultipartFile file = mockCSVFile(csvContent);

      when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_REACTIVATE_USER))).thenReturn(true);
      when(csvValidator.validateReActivateUser(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());
      when(userFilterSortDao.getUserByEmailId(eq("notfound@example.com"), eq(Constants.IN_ACTIVE_STATUS))).thenReturn(null);

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(0, response.getSuccessCount());
      assertEquals(1, response.getFailureCount());
      assertEquals(1, response.getTotalCount());
      assertEquals(1, response.getErrors().size());
    }

    @Test
    void processReActivateUsers_RecordValidationFails() throws Exception {
      String csvContent = "EmailID,ExpiryDate\ninvalid-email,2025-12-31";
      MultipartFile file = mockCSVFile(csvContent);

      when(csvValidator.validateHeaders(anyMap(), eq(Constants.VALID_REACTIVATE_USER))).thenReturn(true);
      when(csvValidator.validateReActivateUser(any(CSVRecord.class), anyInt(), anySet()))
          .thenReturn(List.of("Invalid email format"));

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(0, response.getSuccessCount());
      assertEquals(1, response.getFailureCount());
      assertEquals(1, response.getTotalCount());
      assertEquals(1, response.getErrors().size());
    }

    @Test
    void processReActivateUsers_IOException() throws Exception {
      MultipartFile file = mock(MultipartFile.class);
      when(file.getInputStream()).thenThrow(new IOException("Simulated IO error"));

      CSVProcessResponse response = csvProcessor.processReActivateUsers(file);

      assertEquals(0, response.getSuccessCount());
      assertEquals(1, response.getFailureCount());
      assertEquals(0, response.getTotalCount());
      assertEquals(1, response.getErrors().size());
    }

    // Helper method
    private MultipartFile mockCSVFile(String content) {
      return new MockMultipartFile("file", "test.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

  @Test
  void processDeleteBulkUsers_Success() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "EmailID,FirstName,LastName\n" +
        "john.doe@example.com,John,Doe\n" +
        "jane.doe@example.com,Jane,Doe";

    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateDeleteUserFields(any(CSVRecord.class), anyInt()))
        .thenAnswer(invocation -> {
          CSVRecord record = invocation.getArgument(0);
          int rowNum = invocation.getArgument(1);
          String email = record.get("EmailID").trim().toLowerCase();
          User user = new User();
          user.setEmailId(email);
          return user;
        });

    List<User> users = csvProcessor.processDeleteBulkUsers(file, 1, 2);

    assertEquals(2, users.size());
    List<String> emails = users.stream().map(User::getEmailId).collect(Collectors.toList());
    assertTrue(emails.contains("john.doe@example.com"));
    assertTrue(emails.contains("jane.doe@example.com"));
  }

  @Test
  void processDeleteBulkUsers_InvalidStartRange_ThrowsException() {
    MultipartFile file = mock(MultipartFile.class);
    assertThrows(IllegalArgumentException.class, () -> csvProcessor.processDeleteBulkUsers(file, 0, 5));
  }

  @Test
  void processDeleteBulkUsers_EndLessThanStart_ThrowsException() {
    MultipartFile file = mock(MultipartFile.class);
    assertThrows(IllegalArgumentException.class, () -> csvProcessor.processDeleteBulkUsers(file, 5, 2));
  }
  @Test
  void processDeleteBulkUsers_IOException_ThrowsException() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new IOException("File read error"));

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> csvProcessor.processDeleteBulkUsers(file, 1, 2));

    assertTrue(exception.getMessage().contains("Failed to process the CSV file"));
  }
  @Test
  void processDeleteBulkUsers_AllInvalidRecords_ReturnsEmptyList() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    String csvContent = "EmailID,FirstName,LastName\ninvalid1@example.com,Invalid,User\ninvalid2@example.com,Invalid,User";
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    when(csvValidator.validateDeleteUserFields(any(CSVRecord.class), anyInt())).thenReturn(null);

    List<User> users = csvProcessor.processDeleteBulkUsers(file, 2, 3);

    assertTrue(users.isEmpty());
  }

}