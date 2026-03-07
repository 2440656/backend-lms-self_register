package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettingsValidatorTest {

    private SettingsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SettingsValidator();
    }

    private IntegrationDraftRequest.Settings buildValidSettings() {
        IntegrationDraftRequest.Settings settings = new IntegrationDraftRequest.Settings();
        settings.setAuthenticationMethod("Password Auth");
        settings.setSyncType("Automatic");
        settings.setSyncSchedule("Daily");
        settings.setSyncTime("09:00 AM");
        settings.setWeekDay("Monday");
        IntegrationDraftRequest.UniqueIdentifiers id = new IntegrationDraftRequest.UniqueIdentifiers();
        id.setThirdPartyIdentifier("Third Party 1");
        id.setSkillSpringIdentifier("Skill Spring 1");
        settings.setIdentifiersList(List.of(id));
        return settings;
    }

    @Test
    void testValidSettings() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        assertDoesNotThrow(() -> validator.validate(settings));
    }

    @Test
    void testNullSettingsThrows() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }

    @Test
    void testEmptyAuthenticationMethodThrows() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setAuthenticationMethod(" ");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testSyncTypeNotAlphanumericThrows() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncType("Auto@matic");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testSyncScheduleNotAlphanumericThrows() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncSchedule("Daily!");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testSyncTimeInvalidFormatThrows() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncTime("25:00");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testSyncTimeValid24HourFormat() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncTime("23:59");
        assertDoesNotThrow(() -> validator.validate(settings));
    }

    @Test
    void testSyncTimeValid12HourFormat() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncTime("12:30 pm");
        assertDoesNotThrow(() -> validator.validate(settings));
    }

    @Test
    void testSyncTypeManualSkipsSyncFields() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncType("Manual");
        settings.setSyncSchedule(null);
        settings.setSyncTime(null);
        settings.setWeekDay(null);
        assertDoesNotThrow(() -> validator.validate(settings));
    }

    @Test
    void testSyncScheduleWeeklyRequiresWeekDay() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        settings.setSyncSchedule("Weekly");
        settings.setWeekDay("Monday");
        assertDoesNotThrow(() -> validator.validate(settings));
        settings.setWeekDay("!Monday");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testIdentifierFieldsValidation() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        IntegrationDraftRequest.UniqueIdentifiers id = new IntegrationDraftRequest.UniqueIdentifiers();
        id.setThirdPartyIdentifier("Third Party 1");
        id.setSkillSpringIdentifier("Skill Spring 1");
        settings.setIdentifiersList(List.of(id));
        assertDoesNotThrow(() -> validator.validate(settings));

        id.setThirdPartyIdentifier("Invalid@ID");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }

    @Test
    void testFieldLengthExceededThrows() {
        IntegrationDraftRequest.Settings settings = buildValidSettings();
        String longString = "a".repeat(251);
        settings.setAuthenticationMethod(longString);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(settings));
    }
}
