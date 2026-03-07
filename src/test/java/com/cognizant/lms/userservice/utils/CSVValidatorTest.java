package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.dto.TenantDTO;
import org.junit.jupiter.api.BeforeEach;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CSVValidatorTest {
    @Mock
    private RoleDao roleDao;

    @Mock
    private LookupDao lookupDao;

    @Mock
    private UserFilterSortDao userFilterSortDao;

    @InjectMocks
    private CSVValidator csvValidator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    public void testValidateUpdateUserFields_ValidFields() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_FIRST_NAME, "John",
                Constants.FIELD_LAST_NAME, "Doe",
                Constants.FIELD_EMAIL_ID, "john.doe@example.com",
                Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE, "12/31/2050",
                Constants.FIELD_INSTITUTION_NAME, "Institution",
                Constants.FIELD_COUNTRY, "Country"
        ));
        when(record.get(Constants.FIELD_FIRST_NAME)).thenReturn("John");
        when(record.get(Constants.FIELD_LAST_NAME)).thenReturn("Doe");
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("john.doe@example.com");
        when(record.get(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE)).thenReturn("12/31/2050");
        when(record.get(Constants.FIELD_INSTITUTION_NAME)).thenReturn("Institution");
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("Country");


        List<String> validCountries = List.of("Country", "India", "Germany", "USA");
        when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
                .map(country -> new LookupDto(country))
                .toList());


        Set<String> emailSet = new HashSet<>();
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.ACTIVE_STATUS)).thenReturn(new User());

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateUpdateUserFields_InvalidEmailFormat() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_EMAIL_ID, "invalid-email"
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("invalid-email");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Invalid Email format. The email must end with @[a-z,A-Z].com at row 1", errors.get(0));
    }

    @Test
    public void testValidateUpdateUserFields_MissingEmail() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_EMAIL_ID, ""
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("EmailID is missing at row 1", errors.get(0));
    }

    @Test
    public void testValidateUpdateUserFields_InvalidName() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_FIRST_NAME, "John123"
        ));
        when(record.get(Constants.FIELD_FIRST_NAME)).thenReturn("John123");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("FirstName must contain only alphabetic characters at row 1", errors.get(0));
    }

    @Test
    public void testValidateUpdateUserFields_ExpiredDate() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE, "12/31/2020"
        ));
        when(record.get(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE)).thenReturn("12/31/2020");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("AccountExpiryDate must be a future date at row 1", errors.get(0));
    }

    @Test
    public void testValidateHeaders_ValidHeaders() {
        // Arrange
        Map<String, Integer> headers = Map.of(
                "header1", 0,
                "header2", 1
        );
        List<String> validHeaders = List.of("header1", "header2");

        // Act
        boolean result = csvValidator.validateHeaders(headers, validHeaders);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testValidateHeaders_InvalidHeaders() {
        // Arrange
        Map<String, Integer> headers = Map.of(
                "header1", 0,
                "header3", 1
        );
        List<String> validHeaders = List.of("header1", "header2");

        // Act
        boolean result = csvValidator.validateHeaders(headers, validHeaders);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testValidateAddUserFields_ValidFields() {
        // Arrange
        TenantUtil.setTenantDetails(new TenantDTO("t-1", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_FIRST_NAME, "John",
                Constants.FIELD_LAST_NAME, "Doe",
                Constants.FIELD_EMAIL_ID, "john.doe@example.com",
                Constants.FIELD_USER_TYPE, "Internal",
                Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE, "12/31/2050",
                Constants.FIELD_INSTITUTION_NAME, "",
                Constants.FIELD_COUNTRY, "Germany",
                Constants.FIELD_LOGIN_OPTION, "sso"
        ));
        when(record.get(Constants.FIELD_FIRST_NAME)).thenReturn("John");
        when(record.get(Constants.FIELD_LAST_NAME)).thenReturn("Doe");
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("john.doe@example.com");
        when(record.get(Constants.FIELD_USER_TYPE)).thenReturn("Internal");
        when(record.get(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE)).thenReturn("12/31/2050");
        when(record.get(Constants.FIELD_INSTITUTION_NAME)).thenReturn("");
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("Germany");
        when(record.get(Constants.FIELD_LOGIN_OPTION)).thenReturn("sso");

        Set<String> emailSet = new HashSet<>();
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.ACTIVE_STATUS)).thenReturn(null);
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.IN_ACTIVE_STATUS)).thenReturn(null);

        List<String> validCountries = List.of("Country", "India", "Germany", "USA");
        when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
                .map(country -> new LookupDto(country))
                .toList());

        // Act
        List<String> errors = csvValidator.validateAddUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAddUserFields_InvalidEmailFormat() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_EMAIL_ID, "invalid-email"
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("invalid-email");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateAddUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Invalid Email format. The email invalid-email must match the pattern: [a-zA-Z0-9\\p{L}._%+-\\\\']+@[a-zA-Z0-9\\p{L}-]+\\.[a-zA-Z]{2,} at row 1", errors.get(0));
    }

    @Test
    public void testValidateDeactivateUserFields_ValidFields() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_EMAIL_ID, "john.doe@example.com"
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("john.doe@example.com");

        Set<String> emailSet = new HashSet<>();
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.ACTIVE_STATUS)).thenReturn(new User());
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.IN_ACTIVE_STATUS)).thenReturn(null);

        // Act
        List<String> errors = csvValidator.validateDeactivateUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateDeactivateUserFields_InvalidEmailFormat() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_EMAIL_ID, "invalid-email"
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("invalid-email");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateDeactivateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Invalid Email format. The email must end with @[a-z,A-Z].com at row 1", errors.get(0));
    }

    @Test
    public void testValidateAddUserFields_CaseInsensitiveCountry() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_COUNTRY, "india"
        ));
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("india");

        List<String> validCountries = List.of("India", "Germany", "USA");
        when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
                .map(country -> new LookupDto(country))
                .toList());

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateAddUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAddUserFields_ValidCountry() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_COUNTRY, "India"
        ));
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("India");

        List<String> validCountries = List.of("India", "Germany", "USA");
        when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
                .map(country -> new LookupDto(country))
                .toList());

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateAddUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAddUserFields_InvalidCountry() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_COUNTRY, "InvalidCountry"
        ));
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("InvalidCountry");


        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateAddUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Invalid Country: invalidcountry. Please provide a valid country name at row 1", errors.get(0));
    }

    @Test
    public void testValidateUpdateUserFields_ValidCountry() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_COUNTRY, "Germany"
        ));
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("Germany");

        List<String> validCountries = List.of("India", "Germany", "USA");
        when(lookupDao.getLookupData("Country", null)).thenReturn(validCountries.stream()
                .map(country -> new LookupDto(country))
                .toList());

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateUpdateUserFields_InvalidCountry() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                Constants.FIELD_COUNTRY, "UnknownCountry"
        ));
        when(record.get(Constants.FIELD_COUNTRY)).thenReturn("UnknownCountry");

        Set<String> emailSet = new HashSet<>();

        // Act
        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, emailSet);

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Invalid Country: unknowncountry. Please provide a valid country name at row 1", errors.get(0));
    }

    @Test
    public void testValidateReActivateUser_ValidUser() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
            Constants.FIELD_EMAIL_ID, "john.doe@example.com",
            Constants.USER_EXPIRY_DATE, "12/31/2050"
        ));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("john.doe@example.com");
        when(record.get(Constants.USER_EXPIRY_DATE)).thenReturn("12/31/2050");


        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.ACTIVE_STATUS)).thenReturn(null);
        when(userFilterSortDao.getUserByEmailId("john.doe@example.com", Constants.IN_ACTIVE_STATUS)).thenReturn(new User());

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertTrue(errors.isEmpty());
    }


    @Test
    public void testValidateReActivateUser_MissingEmail() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, ""));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("");

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertEquals(Constants.FIELD_EMAIL_ID + " is missing at row 1", errors.get(0));
    }

    @Test
    public void testValidateReActivateUser_InvalidEmailFormat() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, "invalid-email"));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("invalid-email");

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid Email format"));
    }

    @Test
    public void testValidateReActivateUser_EmailTooLong() {
        String longEmail = "a".repeat(256) + "@example.com";
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, longEmail));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn(longEmail);

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("must be 255 characters or less"));
    }

    @Test
    public void testValidateReActivateUser_UserDoesNotExist() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, "nouser@example.com"));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("nouser@example.com");

        when(userFilterSortDao.getUserByEmailId("nouser@example.com", Constants.ACTIVE_STATUS)).thenReturn(null);
        when(userFilterSortDao.getUserByEmailId("nouser@example.com", Constants.IN_ACTIVE_STATUS)).thenReturn(null);

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Does not exist"));
    }

    @Test
    public void testValidateReActivateUser_UserAlreadyActive() {
        User activeUser = new User();
        activeUser.setStatus(Constants.ACTIVE_STATUS);

        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, "active@example.com"));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("active@example.com");

        when(userFilterSortDao.getUserByEmailId("active@example.com", Constants.ACTIVE_STATUS)).thenReturn(activeUser);

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("already Active"));
    }

    @Test
    public void testValidateReActivateUser_DuplicateEmail() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_EMAIL_ID, "duplicate@example.com"));
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("duplicate@example.com");

        when(userFilterSortDao.getUserByEmailId("duplicate@example.com", Constants.ACTIVE_STATUS)).thenReturn(null);
        when(userFilterSortDao.getUserByEmailId("duplicate@example.com", Constants.IN_ACTIVE_STATUS)).thenReturn(new User());

        Set<String> emailSet = new HashSet<>();
        emailSet.add("duplicate@example.com");

        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Duplicate Email Id"));
    }

    @Test
    public void testValidateReActivateUser_ExpiryDateMissing() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.USER_EXPIRY_DATE, ""));
        when(record.get(Constants.USER_EXPIRY_DATE)).thenReturn("");

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertEquals(Constants.USER_EXPIRY_DATE + " is missing at row 1", errors.get(0));
    }


    @Test
    public void testValidateReActivateUser_ExpiryDateInvalidFormat() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.USER_EXPIRY_DATE, "2025-12-31"));
        when(record.get(Constants.USER_EXPIRY_DATE)).thenReturn("2025-12-31");

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid date format"));
    }

    @Test
    public void testValidateReActivateUser_ExpiryDateNotFuture() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT));
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.USER_EXPIRY_DATE, today));
        when(record.get(Constants.USER_EXPIRY_DATE)).thenReturn(today);

        Set<String> emailSet = new HashSet<>();
        List<String> errors = csvValidator.validateReActivateUser(record, 1, emailSet);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("must be a future date"));
    }

    @Test
    public void testValidateUpdateUserFields_ValidRole() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_ROLE, "Student"));
        when(record.get(Constants.FIELD_ROLE)).thenReturn("Student");
        when(roleDao.getRoles()).thenReturn(List.of(new RoleDto("pk1", "Student", "Student")));

        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, new HashSet<>());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAddUserFields_InvalidRoleConditions() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_ROLE, "Alien,Alien,super-admin");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<RoleDto> validRoles = List.of(
            new RoleDto("pk1", "Student", "Student"),
            new RoleDto("pk2", "Admin", "Admin")
        );
        when(roleDao.getRoles()).thenReturn(validRoles);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRoles).thenReturn(List.of("admin"));

            List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

            errors.forEach(System.out::println);

            assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate Role found: Alien")), "Expected duplicate role error");
            assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid Role: Alien")), "Expected invalid role error");
            assertTrue(errors.stream().anyMatch(e -> e.contains("User does not have permission to assign super-admin role")), "Expected super-admin permission error");
        }
    }

    @Test
    public void testValidateAddUserFields_RoleValidationErrors() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_ROLE, "Alien,Alien,super-admin");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        when(roleDao.getRoles()).thenReturn(List.of(new RoleDto("pk1", "Student", "Student")));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRoles).thenReturn(List.of("admin"));

            List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

            assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate Role found: Alien")));
            assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid Role: Alien")));
            assertTrue(errors.stream().anyMatch(e -> e.contains("User does not have permission to assign super-admin role")));
        }
    }


    @Test
    public void testValidateAddUserFields_InvalidViewOnlyAssignedCourses() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES, "Maybe");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid value for " + Constants.FIELD_VIEWONLY_ASSIGNED_COURSES)));
    }

    @Test
    public void testValidateAddUserFields_InvalidLoginOption() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_LOGIN_OPTION, "oauth");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid Login Option")));
    }

    @Test
    public void testValidateAddUserFields_MissingLoginOption() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_LOGIN_OPTION, "");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("LoginOption is missing")));
    }

    @Test
    public void testValidateAddUserFields_InvalidAndDuplicateCountry() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_COUNTRY, "Atlantis,India,India");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        when(lookupDao.getLookupData(eq("Country"), isNull())).thenReturn(List.of(
            new LookupDto("India"),
            new LookupDto("USA")
        ));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid Country: atlantis")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate Country found: india")));
    }


    @Test
    public void testValidateAddUserFields_InvalidExpiryDateFormat() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE, "31-12-2025");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid date format")));
    }

    @Test
    public void testValidateAddUserFields_PastExpiryDate() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE, "01/01/2020");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("must be a future date")));
    }

    @Test
    public void testValidateUpdateUserFields_ValidViewOnlyAssignedCourses_Y() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES, "Y"));
        when(record.get(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES)).thenReturn("Y");

        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, new HashSet<>());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateUpdateUserFields_ValidViewOnlyAssignedCourses_N() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES, "N"));
        when(record.get(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES)).thenReturn("N");

        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, new HashSet<>());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateUpdateUserFields_ValidLoginOption() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(Constants.FIELD_LOGIN_OPTION, "sso"));
        when(record.get(Constants.FIELD_LOGIN_OPTION)).thenReturn("sso");

        List<String> errors = csvValidator.validateUpdateUserFields(record, 1, new HashSet<>());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAddUserFields_InvalidNames() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(
            Constants.FIELD_FIRST_NAME, "John123",
            Constants.FIELD_LAST_NAME, ""
        );
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

        assertTrue(errors.stream().anyMatch(e -> e.contains(Constants.FIELD_FIRST_NAME + " must contain only alphabetic characters")));
        assertTrue(errors.stream().anyMatch(e -> e.contains(Constants.FIELD_LAST_NAME + " is missing")));
    }

    @Test
    public void testValidateAddUserFields_InvalidUserType() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_USER_TYPE, "Guest");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid User Type")));
    }

    @Test
    public void testValidateAddUserFields_InstitutionNameErrors() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(
            Constants.FIELD_USER_TYPE, "Internal",
            Constants.FIELD_INSTITUTION_NAME, "MIT123"
        );
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("should be empty for Internal users")));
    }

    @Test
    public void testValidateDeleteUserFields_EmailMissing() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_EMAIL_ID, "");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("");

        User result = csvValidator.validateDeleteUserFields(record, 1);
        assertNull(result, "Expected null when email is missing");
    }

    @Test
    public void testValidateDeleteUserFields_InvalidEmailFormat() {
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_EMAIL_ID, "invalid-email");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn("invalid-email");

        User result = csvValidator.validateDeleteUserFields(record, 2);
        assertNull(result, "Expected null for invalid email format");
    }

    @Test
    public void testValidateDeleteUserFields_EmailTooLong() {
        String longEmail = "a".repeat(256) + "@example.com";
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_EMAIL_ID, longEmail);
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn(longEmail);

        User result = csvValidator.validateDeleteUserFields(record, 3);
        assertNull(result, "Expected null for email exceeding max length");
    }


    @Test
    public void testValidateDeleteUserFields_ValidEmail_UserFound() {
        String email = "john.doe@example.com";
        User mockUser = new User();
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_EMAIL_ID, email);
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn(email);
        when(userFilterSortDao.getUserByEmailId(email.toLowerCase(), Constants.ACTIVE_STATUS)).thenReturn(mockUser);

        User result = csvValidator.validateDeleteUserFields(record, 4);
        assertNotNull(result, "Expected user to be returned for valid email");
        assertEquals(mockUser, result, "Expected returned user to match mock");
    }

    @Test
    public void testValidateDeleteUserFields_ValidEmail_UserNotFound() {
        String email = "jane.doe@example.com";
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_EMAIL_ID, email);
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(Constants.FIELD_EMAIL_ID)).thenReturn(email);
        when(userFilterSortDao.getUserByEmailId(email.toLowerCase(), Constants.ACTIVE_STATUS)).thenReturn(null);

        User result = csvValidator.validateDeleteUserFields(record, 5);
        assertNull(result, "Expected null when user not found for valid email");
    }

    @Test
    public void testValidateAddUserFields_SystemAdminRolePermissionDenied() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_ROLE, "system-admin");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<RoleDto> validRoles = List.of(
            new RoleDto("pk1", "system-admin", "System Admin")
        );
        when(roleDao.getRoles()).thenReturn(validRoles);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            // Mock user without super-admin role
            mockedUserContext.when(UserContext::getUserRoles).thenReturn(List.of("admin"));

            // Act
            List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

            // Assert
            assertEquals(1, errors.size());
            assertTrue(errors.stream().anyMatch(e ->
                e.contains("Only super-admin can assign system-admin role at row 1")));
        }
    }

    @Test
    public void testValidateAddUserFields_SystemAdminRolePermissionGranted() {
        // Arrange
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_ROLE, "system-admin");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<RoleDto> validRoles = List.of(
            new RoleDto("pk1", "system-admin", "System Admin")
        );
        when(roleDao.getRoles()).thenReturn(validRoles);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            // Mock user WITH super-admin role
            mockedUserContext.when(UserContext::getUserRoles).thenReturn(List.of("super-admin"));

            // Act
            List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

            // Assert
            assertFalse(errors.stream().anyMatch(e ->
                e.contains("Only super-admin can assign system-admin role")));
        }
    }

    @Test
    public void testValidateAddUserFields_SystemAdminCannotAssignSystemAdmin() {
        // Arrange - System-admin trying to assign system-admin role (should NOT be allowed)
        CSVRecord record = mock(CSVRecord.class);
        Map<String, String> recordMap = Map.of(Constants.FIELD_ROLE, "system-admin");
        when(record.toMap()).thenReturn(recordMap);
        when(record.get(anyString())).thenAnswer(inv -> recordMap.get(inv.getArgument(0)));

        List<RoleDto> validRoles = List.of(
                new RoleDto("pk1", "system-admin", "System Admin")
        );
        when(roleDao.getRoles()).thenReturn(validRoles);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            // Mock user WITH system-admin role (but NOT super-admin)
            mockedUserContext.when(UserContext::getUserRoles).thenReturn(List.of("system-admin"));

            // Act
            List<String> errors = csvValidator.validateAddUserFields(record, 1, new HashSet<>());

            // Assert
            assertTrue(errors.stream().anyMatch(e ->
                            e.contains("Only super-admin can assign system-admin role")),
                    "System-admin should NOT be able to assign system-admin role");
        }
    }

}
