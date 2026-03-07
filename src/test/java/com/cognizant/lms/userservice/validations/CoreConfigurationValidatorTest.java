package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoreConfigurationValidatorTest {

    private CoreConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CoreConfigurationValidator();
    }

    private IntegrationDraftRequest.CoreConfiguration validCoreConfig() {
        IntegrationDraftRequest.CoreConfiguration config = new IntegrationDraftRequest.CoreConfiguration();
        config.setHostName("Host123");
        config.setClientId("Client123");
        config.setOrganizationId("Org123");
        config.setClientSecret("Secret123");
        config.setFields(Collections.singletonList(
                new IntegrationDraftRequest.FieldEntry("Field1", "Value1")
        ));
        return config;
    }

    @Test
    void validate_validCoreConfig_noException() {
        assertDoesNotThrow(() -> validator.validate(validCoreConfig()));
    }

    @Test
    void validate_nullCoreConfig_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("Core configuration cannot be null"));
    }

    @Test
    void validate_multipleInvalidFields_reportsAllErrors() {
        IntegrationDraftRequest.CoreConfiguration config = new IntegrationDraftRequest.CoreConfiguration();
        config.setHostName(""); // empty
        config.setClientId("A".repeat(251)); // too long
        config.setOrganizationId("Org#123"); // invalid chars
        config.setClientSecret(null); // null
        config.setFields(Arrays.asList(
                new IntegrationDraftRequest.FieldEntry("Field1", "Invalid Value!"), // invalid chars
                new IntegrationDraftRequest.FieldEntry("Field2", "") // empty
        ));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Host Name is mandatory"));
        assertTrue(msg.contains("Client ID must not exceed"));
        assertTrue(msg.contains("Organization ID must be alphanumeric only"));
        assertTrue(msg.contains("Client Secret is mandatory"));
        assertTrue(msg.contains("Field1 must be alphanumeric only"));
        assertTrue(msg.contains("Field2 is mandatory"));
    }

    @Test
    void validate_nullFields_valid() {
        IntegrationDraftRequest.CoreConfiguration config = validCoreConfig();
        config.setFields(null);
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    void validate_fieldEntryWithInvalidValue_throwsException() {
        IntegrationDraftRequest.CoreConfiguration config = validCoreConfig();
        config.setFields(Collections.singletonList(
                new IntegrationDraftRequest.FieldEntry("Field1", "Invalid Value!") // contains space and exclamation
        ));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        assertTrue(ex.getMessage().contains("Field1 must be alphanumeric only"));
    }
}
