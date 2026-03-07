package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class SettingsValidator {
    private static final int MAX_LENGTH = 250;
    private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";
    private static final String ALPHANUMERIC_WITH_SPACES_REGEX = "^[a-zA-Z0-9\\s]+$";
    // 12-hour format regex: supports formats like "09:00 AM", "9:00 PM", "12:30 am"
    private static final String TIME_12H_REGEX = "^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM|am|pm)$";

    // 24-hour format regex: supports formats like "09:00", "23:59"
    private static final String TIME_24H_REGEX = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";


    public void validate(IntegrationDraftRequest.Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null.");
        }

        validateField(settings.getAuthenticationMethod(), "Authentication Method", ALPHANUMERIC_WITH_SPACES_REGEX);
        validateField(settings.getSyncType(), "Sync Type", ALPHANUMERIC_REGEX);
        if(settings.getSyncType().equalsIgnoreCase("Automatic")  ) {
            validateField(settings.getSyncSchedule(), "Sync Schedule",ALPHANUMERIC_REGEX);
            validateTimeField(settings.getSyncTime(), "Sync Time");
            if(settings.getSyncSchedule().equalsIgnoreCase("Weekly")) {
                validateField(settings.getWeekDay(), "Week Day",ALPHANUMERIC_REGEX);
            }
        }




        if (settings.getIdentifiersList() != null) {
            for (IntegrationDraftRequest.UniqueIdentifiers identifier : settings.getIdentifiersList()) {
                validateField(identifier.getThirdPartyIdentifier(), "Third Party Identifier", ALPHANUMERIC_WITH_SPACES_REGEX);
                validateField(identifier.getSkillSpringIdentifier(), "Skill Spring Identifier", ALPHANUMERIC_WITH_SPACES_REGEX);
            }
        }
    }

    public void validateField(String value, String fieldName,String regex) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is mandatory.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + MAX_LENGTH + " characters.");
        }
        if (!value.matches(regex)) {
            throw new IllegalArgumentException(fieldName + " must support this regex: " + regex);
        }
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
}
