package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SFTPIntegrationValidator {

  private static final int MAX_LENGTH = 250;
  private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";

  // 12-hour format regex: supports formats like "09:00 AM", "9:00 PM", "12:30 am"
  private static final String TIME_12H_REGEX = "^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM|am|pm)$";

  // 24-hour format regex: supports formats like "09:00", "23:59"
  private static final String TIME_24H_REGEX = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";

  public void sftpIntegrationValidator(SFTPIntegrationReqDto.Configuration configuration) {

    List<String> errors = new ArrayList<>();

    if (configuration == null) {
      errors.add("Core configuration cannot be null.");
    }else {
      validateField(configuration.getProvider(), "Provider", errors);
      validateField(configuration.getSyncType(), "Sync Type", errors);
      validateField(configuration.getLocation(), "SFTP Location", errors);
      validateField(configuration.getUserName(), "Username", errors);
     // validateField(configuration.getPassword(), "SFTP Password", errors);
      validateField(configuration.getHost(), "SFTP Host", errors);
      validateField(configuration.getPort(), "SFTP Post", errors);

    }
      if (!errors.isEmpty()) {
          String errorMsg = String.join(" ", errors);
          log.error("GeneralInformation validation failed: {}", errorMsg);
          throw new IllegalArgumentException(errorMsg);
      }
  }

  private void validateField(String value, String fieldName, List<String> errors) {
    if (value == null || value.trim().isEmpty()) {
      errors.add(fieldName + " is mandatory.");
      return;
    }
    if (value.length() > MAX_LENGTH) {
      errors.add(fieldName + " must not exceed " + MAX_LENGTH + " characters.");
    }
  }

  public void sftpSettingsValidator(SFTPIntegrationReqDto.SftpSettingsDTO settings){
    if (settings == null) {
      throw new IllegalArgumentException("Settings cannot be null.");
    }
    validateTimeField(settings.getSyncTime(), "Sync Time");

  }

  private void validateTimeField(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(fieldName + " is mandatory.");
    }

    // Check if it matches 12-hour or 24-hour format
    boolean is12HourFormat = value.matches(TIME_12H_REGEX);
    boolean is24HourFormat = value.matches(TIME_24H_REGEX);

    if (!is12HourFormat && !is24HourFormat) {
      throw new IllegalArgumentException(fieldName + " must be in HH:MM AM/PM or HH:MM format.");
    }

    // For 12-hour format, just validate the regex match is sufficient
    // since the regex already ensures valid hours (1-12) and minutes (00-59)
    // No need for additional LocalTime parsing for 12-hour format
    try {
      if (is24HourFormat) {
        // Only parse 24-hour format with LocalTime
        LocalTime.parse(value);
      }
      // For 12-hour format, regex validation is sufficient
    } catch (DateTimeParseException e) {
      log.error("Time parsing failed for value: {} with error: {}", value, e.getMessage());
      throw new IllegalArgumentException(fieldName + " must be a valid time format.");
    }
  }

  public void CategoryMapperValidator(SFTPIntegrationReqDto.CategoryMapping categoryMapping){
    List<String> errors = new ArrayList<>();
    if (categoryMapping == null) {
      errors.add("Category mapping cannot be null.");
    }else if(categoryMapping.getCategoryTypeMappings() != null){
      for(SFTPIntegrationReqDto.SftpCategoryTypeMapping categoryTypeMapping : categoryMapping.getCategoryTypeMappings()) {
        validateFieldWithAlphanumericValue(categoryTypeMapping.getSkillSpringCategoryType(),categoryTypeMapping.getThirdPartyCategoryType(),ALPHANUMERIC_REGEX,  errors);
      }
    }
  }

  private void validateFieldWithAlphanumericValue(String value, String fieldName, String regex, List<String> errors) {
    if (value == null || value.trim().isEmpty()) {
      errors.add(fieldName + " is mandatory.");
      return;
    }
    if (value.length() > MAX_LENGTH) {
      errors.add(fieldName + " must not exceed " + MAX_LENGTH + " characters.");
    }
    if (!value.matches(regex)) {
      errors.add(fieldName + " must be alphanumeric only.");
    }
  }
}
