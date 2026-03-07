package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.CSVProcessResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cognizant.lms.userservice.dto.LookupDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Component
public class CSVProcessor {

  private CSVValidator csvValidator;

  private LookupDao lookupDao;

  @Autowired
  private UserFilterSortDao userFilterSortDao;

  @Autowired
  public void setCountryDao(LookupDao lookupDao) {
    this.lookupDao = lookupDao;
  }

  public CSVProcessor(CSVValidator csvValidator) {
    this.csvValidator = csvValidator;
  }

  public CSVProcessResponse processReActivateUsers(MultipartFile file) {
    Set<String> validEmail = new HashSet<>();
    List<User> validUsers = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int totalCount = 0;
    int failureCount = 0;
    int successCount = 0;
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
    String createdOn = utcDateTime.format(formatter);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
        StandardCharsets.ISO_8859_1))) {
      CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader);
      boolean validationHeaders =
          csvValidator.validateHeaders(csvParser.getHeaderMap(), Constants.VALID_REACTIVATE_USER);

      if (validationHeaders) {
        int rowNum = Constants.INITIAL_ROW_NUM;
        for (CSVRecord record : csvParser) {
          rowNum++;
          totalCount++;
          log.info("Processing row number: " + rowNum);
          log.info("Record details: " + record.toString());
          if (totalCount > 50) {
            failureCount = failureCount + 1;
            log.info("failureCount is " + failureCount);
            errors.add(String.format(
                "%s--%s",
                LocalDateTime.now().format(DateTimeFormatter
                    .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                "CSV file contains more than 50 records"));
            break;
          }
          List<String> errorList =
              csvValidator.validateReActivateUser(record, rowNum, validEmail);
          if (errorList.isEmpty()) {
            String lowerCaseEmail = record.get(Constants.FIELD_EMAIL_ID).trim();
            String userAccountExpiryDate = record.get("ExpiryDate");
            User inactiveUser = userFilterSortDao.getUserByEmailId(lowerCaseEmail.trim(),
                Constants.IN_ACTIVE_STATUS);
            if (inactiveUser != null) {
              inactiveUser.setStatus(Constants.ACTIVE_STATUS);
              inactiveUser.setUserAccountExpiryDate(userAccountExpiryDate);
              inactiveUser.setRole(Constants.ROLE_LEARNER);
              inactiveUser.setReactivatedDate(createdOn);
              validUsers.add(inactiveUser);
              validEmail.add(inactiveUser.getEmailId());
              successCount = successCount + 1;
            } else {
              failureCount = failureCount + 1;
              errors.add(String.format(
                  "%s--    %s--    %s",
                  LocalDateTime.now().format(DateTimeFormatter
                      .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                  record.get(Constants.FIELD_EMAIL_ID),
                  "User not found"));
            }
          } else {
            failureCount = failureCount + 1;
            errorList.forEach(err -> errors.add(
                String.format(
                    "%s--%s--%s--%s",
                    LocalDateTime.now().format(DateTimeFormatter
                        .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    record.get(Constants.FIELD_EMAIL_ID),
                    record.get("ExpiryDate"),
                    err)));
            log.info("failureCount is " + failureCount);
          }
        }
      } else {
        log.info("failureCount is " + failureCount);
        errors.add(String.format(
            "%s--%s",
            LocalDateTime.now().format(DateTimeFormatter
                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
            "Invalid Header: Must be one of " + Constants.VALID_REACTIVATE_USER));
      }
    } catch (IOException e) {
      log.error("Error while processing the file: " + e.getMessage());
      failureCount = failureCount + 1;
      errors.add(String.format(
          "%s--%s",
          LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
          "Error while processing the file: " + e.getMessage()));
    }
    log.info("Processor Total count: " + totalCount);
    log.info("Processor Success count: " + successCount);
    log.info("Processor Failure count: " + failureCount);
    return new CSVProcessResponse(validUsers, errors, successCount, failureCount, totalCount);
  }

  public CSVProcessResponse processAddUserFile(MultipartFile file) {

    Set<String> emailSet = new HashSet<>();
    List<User> validUsers = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int totalCount = 0;
    int failureCount = 0;
    int successCount = 0;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
        StandardCharsets.ISO_8859_1))) {
      CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader);
      boolean validationHeaders =
          csvValidator.validateHeaders(csvParser.getHeaderMap(), Constants.VALID_ADD_USERS_HEADERS);
      if (validationHeaders) {
        int rowNum = Constants.INITIAL_ROW_NUM;
        for (CSVRecord record : csvParser) {
          record.get(Constants.FIELD_LAST_NAME);
          rowNum++;
          totalCount++;
          log.info("Processing row number: " + rowNum);
          log.info("Record details: " + record.toString());
          if (totalCount > 50) {
            failureCount = failureCount + 1;
            log.info("failureCount is " + failureCount);
            errors.add(String.format(
                "%s--%s",
                LocalDateTime.now().format(DateTimeFormatter
                    .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                "CSV file contains more than 50 records"));
            break;
          }
          List<String> errorList =
              csvValidator.validateAddUserFields(record, rowNum, emailSet);
          log.info("Error list: {}", errorList);
          if (errorList.isEmpty()) {

            User user = User.builder()
                .firstName(record.get(Constants.FIELD_FIRST_NAME))
                .lastName(record.get(Constants.FIELD_LAST_NAME))
                .institutionName(record.get(Constants.FIELD_INSTITUTION_NAME))
                .emailId(record.get(Constants.FIELD_EMAIL_ID).trim())
                .userType(capitalize(record.get(Constants.FIELD_USER_TYPE)))
                .role(record.get(Constants.FIELD_ROLE))
                .userAccountExpiryDate(record.get(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE))
                .viewOnlyAssignedCourses(record.get(Constants.FIELD_VIEWONLY_ASSIGNED_COURSES))
                .loginOption(record.get(Constants.FIELD_LOGIN_OPTION))
                .country(
                    lookupDao.getLookupData("Country", null).stream()
                        .filter(c -> c.getName().equalsIgnoreCase(record.get(Constants.FIELD_COUNTRY).trim()))
                        .map(LookupDto::getName)
                        .findFirst()
                        .orElse(null)
                )
                .build();
            if (user.getCountry() == null) {
              errors.add(String.format(
                  "%s--%s--%s--%s--Invalid Country: %s",
                  LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                  record.get(Constants.FIELD_EMAIL_ID),
                  record.get(Constants.FIELD_FIRST_NAME),
                  record.get(Constants.FIELD_LAST_NAME),
                  record.get(Constants.FIELD_COUNTRY)
              ));
              failureCount++;
              continue;
            }

            validUsers.add(user);
            successCount = successCount + 1;
            log.info("successCount is " + successCount);
          } else {
            failureCount = failureCount + 1;
            errorList.forEach(err -> errors.add(
                String.format(
                    "%s--%s--%s--%s--%s",
                    LocalDateTime.now().format(DateTimeFormatter
                        .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    record.get(Constants.FIELD_EMAIL_ID),
                    record.get(Constants.FIELD_FIRST_NAME), record.get(Constants.FIELD_LAST_NAME),
                    err)));
            log.info("failureCount is " + failureCount);
          }
        }
      } else {
        log.info("failureCount is " + failureCount);
        errors.add(String.format(
            "%s--%s",
            LocalDateTime.now().format(DateTimeFormatter
                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
            "Invalid Header: Must be one of " + Constants.VALID_ADD_USERS_HEADERS));
      }
    } catch (Exception e) {
      log.error("Error while processing the file: " + e.getMessage());
      failureCount = failureCount + 1;
      errors.add(String.format(
          "%s--%s",
          LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
          "Error while processing the file: " + e.getMessage()));
    }
    log.info("Processor Total count: " + totalCount);
    log.info("Processor Success count: " + successCount);
    log.info("Processor Failure count: " + failureCount);
    return new CSVProcessResponse(validUsers, errors, successCount, failureCount, totalCount);
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  public CSVProcessResponse processDeActiveUserFile(MultipartFile file) {
    List<String> errors = new ArrayList<>();
    Set<String> validEmails = new HashSet<>();
    int totalCount = 0;
    int failureCount = 0;
    int successCount = 0;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
        StandardCharsets.UTF_8))) {
      CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader);
      log.info("Headers in CSV file: {}", csvParser.getHeaderMap().keySet());
      log.info("Expected headers: {}", Constants.VALID_UPDATE_USERS_HEADERS);
      boolean validationHeaders =
          csvValidator.validateHeaders(csvParser.getHeaderMap(),
              Constants.VALID_DEACTIVATE_USERS_HEADERS);
      if (validationHeaders) {
        int rowNum = Constants.INITIAL_ROW_NUM;
        for (CSVRecord record : csvParser) {
          rowNum++;
          totalCount++;
          log.info("Processing row number: " + rowNum);
          log.info("Record details: " + record.toString());
          if (totalCount > 50) {
            failureCount = failureCount + 1;
            log.info("failureCount is " + failureCount);
            errors.add(String.format(
                "%s--%s",
                LocalDateTime.now().format(DateTimeFormatter
                    .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                "CSV file contains more than 50 records"));
            break;
          }
          List<String> errorList =
              csvValidator.validateDeactivateUserFields(record, rowNum, validEmails);
          if (errorList.isEmpty()) {
            successCount = successCount + 1;
            log.info("successCount is " + successCount);
          } else {
            failureCount = failureCount + 1;
            errorList.forEach(err -> errors.add(
                String.format(
                    "%s--%s--%s",
                    LocalDateTime.now().format(DateTimeFormatter
                        .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    record.get(Constants.FIELD_EMAIL_ID),
                    err)));
            log.info("failureCount is " + failureCount);
          }
        }
      } else {
        log.info("failureCount is " + failureCount);
        errors.add(String.format(
            "%s--%s",
            LocalDateTime.now().format(DateTimeFormatter
                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
            "Invalid Header: Must be one of " + Constants.VALID_DEACTIVATE_USERS_HEADERS));
      }
    } catch (Exception e) {
      log.error("Error while processing the file: " + e.getMessage());
      failureCount = failureCount + 1;
      errors.add(String.format(
          "%s--%s",
          LocalDateTime.now().format(DateTimeFormatter
              .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
          "Error while processing the file: " + e.getMessage()));
    }
    log.info("Processor Total count: " + totalCount);
    log.info("Processor Success count: " + successCount);
    log.info("Processor Failure count: " + failureCount);
    return new CSVProcessResponse(validEmails, errors, successCount, failureCount, totalCount);
  }

  public CSVProcessResponse processUpdateUserFile(MultipartFile file) {

    Set<String> emailSet = new HashSet<>();
    List<User> validUsers = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int totalCount = 0;
    int failureCount = 0;
    int successCount = 0;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
        StandardCharsets.UTF_8))) {
      CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader);
      boolean validationHeaders =
          csvValidator.validateHeaders(csvParser.getHeaderMap(),
              Constants.VALID_UPDATE_USERS_HEADERS);
      if (validationHeaders) {
        int rowNum = Constants.INITIAL_ROW_NUM;
        List<String> validCountries = lookupDao.getLookupData("Country", null).stream()
            .map(LookupDto::getName)
            .toList();

        for (CSVRecord record : csvParser) {
          rowNum++;
          totalCount++;
          log.info("Processing row number: " + rowNum);
          log.info("Record details: " + record.toString());
          if (totalCount > 50) {
            failureCount = failureCount + 1;
            log.info("failureCount is " + failureCount);
            errors.add(String.format(
                "%s--%s",
                LocalDateTime.now().format(DateTimeFormatter
                    .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                "CSV file contains more than 50 records"));
            break;
          }
          List<String> errorList =
              csvValidator.validateUpdateUserFields(record, rowNum, emailSet);
          if (errorList.isEmpty()) {
            String csvCountry = record.get(Constants.FIELD_COUNTRY).trim().toLowerCase();
            String updatedCountry = validCountries.stream()
                .filter(c -> c.equalsIgnoreCase(csvCountry))
                .findFirst()
                .orElse(null);


            if (updatedCountry == null) {
              errors.add(String.format(
                  "%s--%s--%s--%s--Invalid Country: %s. Must be one of %s",
                  LocalDateTime.now().format(DateTimeFormatter
                      .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                  record.get(Constants.FIELD_EMAIL_ID),
                  record.get(Constants.FIELD_FIRST_NAME),
                  record.get(Constants.FIELD_LAST_NAME),
                  csvCountry,
                  validCountries));
              failureCount++;
              continue;
            }

            User user = new User();
            user.setFirstName(capitalize(record.get(Constants.FIELD_FIRST_NAME)));
            user.setLastName(capitalize(record.get(Constants.FIELD_LAST_NAME)));
            user.setInstitutionName(record.get(Constants.FIELD_INSTITUTION_NAME));
            user.setEmailId(record.get(Constants.FIELD_EMAIL_ID));
            user.setUserAccountExpiryDate(record.get(Constants.FIELD_USER_ACCOUNT_EXPIRY_DATE));
            user.setCountry(updatedCountry);
            validUsers.add(user);
            successCount = successCount + 1;
            log.info("successCount is " + successCount);
          } else {
            failureCount = failureCount + 1;
            errorList.forEach(err -> errors.add(
                String.format(
                    "%s--%s--%s--%s--%s",
                    LocalDateTime.now().format(DateTimeFormatter
                        .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    record.get(Constants.FIELD_EMAIL_ID),
                    record.get(Constants.FIELD_FIRST_NAME), record.get(Constants.FIELD_LAST_NAME),
                    err)));
            log.info("failureCount is " + failureCount);
          }
        }
      } else {
        log.info("failureCount is " + failureCount);
        errors.add(String.format(
            "%s--%s",
            LocalDateTime.now().format(DateTimeFormatter
                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
            "Invalid Header: Must be one of " + Constants.VALID_UPDATE_USERS_HEADERS));
      }
    } catch (Exception e) {
      log.error("Error while processing the file: " + e.getMessage());
      failureCount = failureCount + 1;
      errors.add(String.format(
          "%s--%s",
          LocalDateTime.now().format(DateTimeFormatter
              .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
          "Error while processing the file: " + e.getMessage()));
    }
    log.info("Processor Total count: " + totalCount);
    log.info("Processor Success count: " + successCount);
    log.info("Processor Failure count: " + failureCount);
    return new CSVProcessResponse(validUsers, errors, successCount, failureCount, totalCount);
  }

  public List<User> processDeleteBulkUsers(MultipartFile file, int start, int end) {
    if (start < 1 || end < start) {
      throw new IllegalArgumentException("Invalid row range provided. Start must be > 1 and end must be >= start.");
    }

    int rangeSize = end - start + 1;
    int initialCapacity = (int) (rangeSize / 0.75f) + 1;
    Map<String, User> userMap = new HashMap<>(initialCapacity);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

      CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
          .setHeader()
          .setSkipHeaderRecord(true)
          .build();

      CSVParser csvParser = csvFormat.parse(reader);
      log.info("Headers in CSV file: {}", csvParser.getHeaderMap().keySet());

      int rowNum = 1;
      for (CSVRecord record : csvParser) {
        if (rowNum < start) {
          rowNum++;
          continue;
        }
        if (rowNum > end) {
          break;
        }

        log.info("Processing row number: {}", rowNum);
        log.debug("Record details: {}", record);

        User user = csvValidator.validateDeleteUserFields(record, rowNum);
        if (user != null) {
          userMap.put(user.getEmailId().trim().toLowerCase(), user);
        }
        rowNum++;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to process the CSV file.", e);
    }
    return new ArrayList<>(userMap.values());
  }
}
