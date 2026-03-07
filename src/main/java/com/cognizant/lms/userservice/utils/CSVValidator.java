package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.dto.RoleDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CSVValidator {
  private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.EMAIL_PATTERN);

  @Autowired
  private RoleDao roleDao;

  @Autowired
  private UserFilterSortDao userFilterSortDao;

  @Autowired
  private LookupDao lookupDao;

  public List<String> validateUpdateUserFields(CSVRecord record, int rowNum,
                                               Set<String> emailSet) {
    List<String> fieldErrors = new ArrayList<>();
    for (String columnHeader : record.toMap().keySet()) {
      switch (columnHeader) {
        case Constants.FIELD_FIRST_NAME, Constants.FIELD_LAST_NAME -> {
          if (!(record.get(columnHeader) == null || record.get(columnHeader).isEmpty())) {
            if (!record.get(columnHeader).matches("[a-zA-Z ]+")) {
              fieldErrors.add(
                  columnHeader + " must contain only alphabetic characters at row " + rowNum);
            } else if (record.get(columnHeader).length() > Constants.MAX_NAME_LENGTH) {
              fieldErrors.add(columnHeader + " must be 100 characters or less at row " + rowNum);
            }
          }
        }
        case Constants.FIELD_EMAIL_ID -> {
          String email = record.get(columnHeader);
          if (email == null || email.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            fieldErrors.add("Invalid Email format."
                + " The email must end with @[a-z,A-Z].com at row " + rowNum);
          } else if (email.length() > Constants.MAX_EMAIL_LENGTH) {
            fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
          } else {
            String lowerCaseEmail = email.toLowerCase();
            User activeUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.ACTIVE_STATUS);
            User inactiveUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.IN_ACTIVE_STATUS);
            if (activeUser == null && inactiveUser == null) {
              fieldErrors.add("User with email id " + email + " does not exist");
            } else {
              if (inactiveUser != null) {
                fieldErrors.add("User with email id " + email + " is already an inactive user");
              }
              if (activeUser != null && !emailSet.add(lowerCaseEmail)) {
                fieldErrors.add("Duplicate Email Id found: " + email
                    + " at row " + rowNum);
              }
            }
          }
        }
        case Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE -> {
          if (record.get(columnHeader) != null && !record.get(columnHeader).trim().isEmpty()) {
            DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
            try {
              LocalDate expiryDate = LocalDate.parse(record.get(columnHeader), formatter);
              if (!expiryDate.isAfter(LocalDate.now())) {
                fieldErrors.add(columnHeader + " must be a future date at row " + rowNum);
              }
            } catch (DateTimeParseException e) {
              fieldErrors.add("Invalid date format for " + columnHeader + " at row "
                  + rowNum + ". Expected format: MM/dd/yyyy");
            }
          }
        }
        case Constants.FIELD_INSTITUTION_NAME -> {
          if (!(record.get(columnHeader) == null || record.get(columnHeader).isEmpty())) {
            if (!record.get(columnHeader).matches("[a-zA-Z]*")) {
              fieldErrors.add(
                  columnHeader + " must contain only alphabetic characters at row " + rowNum);
            } else if (record.get(columnHeader).length() > Constants.MAX_INSTITUTION_NAME_LENGTH) {
              fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
            }
          }
        }
        case Constants.FIELD_COUNTRY -> {
          String country = record.get(columnHeader);
          List<String> validCountries = lookupDao.getLookupData("Country", null).stream()
              .map(LookupDto::getName)
              .map(String::toLowerCase)
              .toList();
          if (country == null || country.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else {
            List<String> countries = country.contains(",") ? List.of(country.split(",")) : List.of(country);
            Set<String> countrySet = new HashSet<>();
            for (String c : countries) {
              String trimmedCountry = c.trim().toLowerCase(); //Convert CSV country to lowercase
              if (!validCountries.contains(trimmedCountry)) {
                fieldErrors.add("Invalid Country: " + trimmedCountry + ". Please provide a valid country name at row " + rowNum);
              } else if (!countrySet.add(trimmedCountry)) {
                fieldErrors.add("Duplicate Country found: " + trimmedCountry + " at row " + rowNum);
              }
            }
            if (countries.size() > 1 && fieldErrors.isEmpty()) {
              fieldErrors.add("A user cannot have more than one country at row " + rowNum);
            }
          }

        }
        default -> {
        }
      }
    }
    return fieldErrors;
  }

  public boolean validateHeaders(Map<String, Integer> headers, List<String> validHeaders) {
    return headers.keySet().containsAll(validHeaders) && new HashSet<>(validHeaders).containsAll(headers.keySet());
  }

  public List<String> validateReActivateUser(CSVRecord record, int rowNum, Set<String> validEmail) {
    log.info("inside the validation method !!!!");
    java.util.List<java.lang.String> fieldErrors = new ArrayList<>();
    for (java.lang.String columnHeader : record.toMap().keySet()) {
      switch (columnHeader) {
        case Constants.FIELD_EMAIL_ID -> {
          java.lang.String email = record.get(columnHeader).trim();
          if (email.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            fieldErrors.add("Invalid Email format. The email " + email + " must match the pattern: [a-zA-Z0-9\\p{L}._%+-\\\\']+@[a-zA-Z0-9\\p{L}-]+\\.[a-zA-Z]{2,} at row " + rowNum);
          } else if (email.length() > Constants.MAX_EMAIL_LENGTH) {
            fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
          } else {
            String lowerCaseEmail = email.toLowerCase().trim();
            User activeUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.ACTIVE_STATUS);
            User inActiveuser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.IN_ACTIVE_STATUS);
            if (activeUser == null && inActiveuser == null) {
              fieldErrors.add("User with Email " + lowerCaseEmail + " Does not exist.");
            }

            if (activeUser != null && activeUser.getStatus().equalsIgnoreCase(Constants.ACTIVE_STATUS)) {
              fieldErrors.add("User is already Active");
            } else {
              if (!validEmail.add(lowerCaseEmail)) {
                fieldErrors.add("Duplicate Email Id found: " + email
                    + " at row " + rowNum);
              }
            }
          }
        }
        case Constants.USER_EXPIRY_DATE -> {
          if (record.get(columnHeader) == null || record.get(columnHeader).isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          }
          if (record.get(columnHeader) != null && !record.get(columnHeader).trim().isEmpty()) {
            DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
            try {
              LocalDate expiryDate = LocalDate.parse(record.get(columnHeader), formatter);
              if (!expiryDate.isAfter(LocalDate.now())) {
                fieldErrors.add(columnHeader + " must be a future date at row " + rowNum);
              }
            } catch (DateTimeParseException e) {
              fieldErrors.add("Invalid date format for " + columnHeader + " at row "
                  + rowNum + ". Expected format: MM/dd/yyyy");
            }
          }
        }

      }
    }
    return fieldErrors;
  }


  public List<String> validateAddUserFields(CSVRecord record, int rowNum,
                                            Set<String> emailSet) {
    List<String> fieldErrors = new ArrayList<>();
    for (String columnHeader : record.toMap().keySet()) {
      switch (columnHeader) {
        case Constants.FIELD_FIRST_NAME, Constants.FIELD_LAST_NAME -> {
          if (record.get(columnHeader) == null || record.get(columnHeader).isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!record.get(columnHeader).matches("[a-zA-Z' ]+")) {
            fieldErrors.add(
                columnHeader + " must contain only alphabetic characters at row " + rowNum);
          } else if (record.get(columnHeader).length() > Constants.MAX_NAME_LENGTH) {
            fieldErrors.add(columnHeader + " must be 100 characters or less at row " + rowNum);
          }
        }
        case Constants.FIELD_EMAIL_ID -> {
          String email = record.get(columnHeader).trim();


          if (email == null || email.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            fieldErrors.add("Invalid Email format. The email " + email + " must match the pattern: [a-zA-Z0-9\\p{L}._%+-\\\\']+@[a-zA-Z0-9\\p{L}-]+\\.[a-zA-Z]{2,} at row " + rowNum);
          } else if (email.length() > Constants.MAX_EMAIL_LENGTH) {
            fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
          } else {
            String lowerCaseEmail = email.toLowerCase();
            User activeUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.ACTIVE_STATUS);
            User inactiveUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.IN_ACTIVE_STATUS);
            if (activeUser != null || inactiveUser != null) {
              fieldErrors.add("User with email id " + email + " already exists");
            } else {
              if (!emailSet.add(lowerCaseEmail)) {
                fieldErrors.add("Duplicate Email Id found: " + email
                    + " at row " + rowNum);
              }
            }
          }
        }
        case Constants.FIELD_USER_TYPE -> {
          if (record.get(columnHeader) == null || record.get(columnHeader).isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!Constants.USER_TYPES.contains(record.get(columnHeader))) {
            fieldErrors.add("Invalid User Type. Must be Internal or External at row " + rowNum);
          }
        }
        case Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE -> {
          if (record.get(columnHeader) != null && !record.get(columnHeader).trim().isEmpty()) {
            DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
            try {
              LocalDate expiryDate = LocalDate.parse(record.get(columnHeader), formatter);
              if (!expiryDate.isAfter(LocalDate.now())) {
                fieldErrors.add(columnHeader + " must be a future date at row " + rowNum);
              }
            } catch (DateTimeParseException e) {
              fieldErrors.add("Invalid date format for " + columnHeader + " at row "
                  + rowNum + ". Expected format: MM/dd/yyyy");
            }
          }
        }
        case Constants.FIELD_INSTITUTION_NAME -> {
          if (Constants.USER_TYPE_INTERNAL.equalsIgnoreCase(record.get(Constants.FIELD_USER_TYPE))
              && !record.get(columnHeader).isEmpty()) {
            fieldErrors.add(columnHeader + " should be empty for Internal users at row " + rowNum);
          } else if (
              Constants.USER_TYPE_EXTERNAL.equalsIgnoreCase(record.get(Constants.FIELD_USER_TYPE))
                  &&
                  (record.get(columnHeader) == null || record.get(columnHeader).isEmpty())) {
            fieldErrors.add(
                columnHeader + " should not be empty for External users at row " + rowNum);
          } else if (!record.get(columnHeader).matches("[a-zA-Z0-9 ]*")) {
            fieldErrors.add(
                columnHeader + " must contain only alphabetic characters, numbers, or spaces at row " + rowNum);
          } else if (record.get(columnHeader).length() > Constants.MAX_INSTITUTION_NAME_LENGTH) {
            fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
          }
        }
        case Constants.FIELD_ROLE -> {
          String roles = record.get(columnHeader);
          List<String> rolesList;
          List<RoleDto> rolesMap = roleDao.getRoles();
          rolesList = rolesMap.stream().map(RoleDto::getSk).toList();
          log.info("Roles List from db : {}", rolesList);
          if (roles != null && !roles.isEmpty()) {
            List<String> roleList =
                roles.contains(", ") ? List.of(roles.split(", ")) :
                    roles.contains(",") ? List.of(roles.split(",")) :
                        List.of(roles);
            Set<String> roleSet = new HashSet<>();
            for (String role : roleList) {
              if (role == null || role.isEmpty()) {
                fieldErrors.add("Role is missing at row " + rowNum);
              }
              if (!roleSet.add(role)) {
                fieldErrors.add("Duplicate Role found: " + role + " at row " + rowNum);
              }
              if (rolesList.stream().noneMatch(r -> r.equalsIgnoreCase(role.trim()))) {
                fieldErrors.add(
                    "Invalid Role: " + role + ". Must be one of " + Constants.FIELD_ROLES
                        + " at row " + rowNum);
              }

              if (role != null && role.equalsIgnoreCase(Constants.SUPER_ADMIN_ROLE) && UserContext.getUserRoles().stream().noneMatch(x -> x.equalsIgnoreCase(Constants.SUPER_ADMIN_ROLE))) {
                fieldErrors.add("User does not have permission to assign super-admin role at row " + rowNum);
              }
              log.info("Checking for system admin role assignment: {}", UserContext.getUserRoles());
              log.info("Role of the user: {}", role);
              if (role != null && role.equalsIgnoreCase(Constants.SYSTEM_ADMIN_ROLE) && UserContext.getUserRoles().stream().noneMatch(x -> x.equalsIgnoreCase(Constants.SUPER_ADMIN_ROLE))) {
                  log.info("Inside system admin role check: {}", role);
                fieldErrors.add("Only super-admin can assign system-admin role at row " + rowNum);
              }
            }
          }
        }
        case Constants.FIELD_VIEWONLY_ASSIGNED_COURSES -> {
          if (record.get(columnHeader) != null && !record.get(columnHeader).isEmpty()) {
            if (!record.get(columnHeader).equalsIgnoreCase("Y")
                && !record.get(columnHeader).equalsIgnoreCase("N")) {
              fieldErrors.add("Invalid value for " + columnHeader + ". Must be Y or N at row "
                  + rowNum);
            }
          }
        }
        case Constants.FIELD_LOGIN_OPTION -> {
          String loginOption = record.get(columnHeader);
          if (loginOption == null || loginOption.isEmpty()) {
            fieldErrors.add(String.format("%s is missing at row %d", columnHeader, rowNum));
          } else {
            List<String> idpPreferences = Arrays.stream(TenantUtil.getTenantDetails().getIdpPreferences().toLowerCase().split(","))
                .map(String::trim)
                .toList();
            if (!idpPreferences.contains(loginOption.toLowerCase())) {
              fieldErrors.add(String.format("Invalid Login Option at row %d. Valid options are: %s", rowNum, String.join(", ", idpPreferences)));
            }
          }
        }
        case Constants.FIELD_COUNTRY -> {
          String country = record.get(columnHeader);
          List<String> validCountries = lookupDao.getLookupData("Country", null).stream()
              .map(LookupDto::getName)
              .map(String::toLowerCase)
              .toList();
          if (country == null || country.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else {
            List<String> countries = country.contains(",") ? List.of(country.split(",")) : List.of(country);
            Set<String> countrySet = new HashSet<>();
            for (String c : countries) {
              String trimmedCountry = c.trim().toLowerCase(); // Convert CSV country to lowercase
              if (!validCountries.contains(trimmedCountry)) {
                fieldErrors.add("Invalid Country: " + trimmedCountry + ". Please provide a valid country name at row " + rowNum);
              } else if (!countrySet.add(trimmedCountry)) {
                fieldErrors.add("Duplicate Country found: " + trimmedCountry + " at row " + rowNum);
              }
            }
            if (countries.size() > 1 && fieldErrors.isEmpty()) {
              fieldErrors.add("A user cannot have more than one country at row " + rowNum);
            }
          }

        }

        default -> {
        }
      }
    }
    return fieldErrors;
  }

  public List<String> validateDeactivateUserFields(CSVRecord record, int rowNum,
                                                   Set<String> emailSet) {
    List<String> fieldErrors = new ArrayList<>();
    for (String columnHeader : record.toMap().keySet()) {
      switch (columnHeader) {
        case Constants.FIELD_EMAIL_ID -> {
          String email = record.get(columnHeader);
          if (email == null || email.isEmpty()) {
            fieldErrors.add(columnHeader + " is missing at row " + rowNum);
          } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            fieldErrors.add("Invalid Email format."
                + " The email must end with @[a-z,A-Z].com at row " + rowNum);
          } else if (email.length() > Constants.MAX_EMAIL_LENGTH) {
            fieldErrors.add(columnHeader + " must be 255 characters or less at row " + rowNum);
          } else {
            String lowerCaseEmail = email.toLowerCase();
            User activeUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.ACTIVE_STATUS);
            User inactiveUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail,
                Constants.IN_ACTIVE_STATUS);
            if (activeUser == null && inactiveUser == null) {
              fieldErrors.add("User with email id " + email + " does not exist");
            } else {
              if (inactiveUser != null) {
                fieldErrors.add("User with email id " + email + " is already an inactive user");
              }
              if (activeUser != null && !emailSet.add(lowerCaseEmail)) {
                fieldErrors.add("Duplicate Email Id found: " + email
                    + " at row " + rowNum);
              }
            }
          }
        }
        default -> {
        }
      }
    }
    return fieldErrors;
  }

  public User validateDeleteUserFields(CSVRecord record, int rowNum) {
    for (String columnHeader : record.toMap().keySet()) {
      if (columnHeader.equals(Constants.FIELD_EMAIL_ID)) {
        String email = record.get(columnHeader);
        if (email == null || email.isEmpty()) {
          log.info("Email is missing at row {}", rowNum);
          return null;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
          log.info("Invalid Email format. The email must end with @[a-z,A-Z].com at row {}", rowNum);
          return null;
        } else if (email.length() > Constants.MAX_EMAIL_LENGTH) {
          log.info("Email must be 255 characters or less at row {}", rowNum);
          return null;
        } else {
          String lowerCaseEmail = email.trim().toLowerCase();
          User user = userFilterSortDao.getUserByEmailId(lowerCaseEmail, Constants.ACTIVE_STATUS);
          log.info("User fetched for deletion: {}", user);
          return user;
        }
      }
    }
    return null;
  }
}
