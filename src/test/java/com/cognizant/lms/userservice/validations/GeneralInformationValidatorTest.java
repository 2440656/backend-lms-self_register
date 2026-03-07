package com.cognizant.lms.userservice.validations;


import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneralInformationValidatorTest {

    private GeneralInformationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GeneralInformationValidator();
    }

    private IntegrationDraftRequest.GeneralInformation validGeneralInfo() {
        IntegrationDraftRequest.GeneralInformation info = new IntegrationDraftRequest.GeneralInformation();
        info.setProvider("Provider1");
        info.setIntegrationId("Integration123");
        info.setIntegrationOwner("Owner Name");
        return info;
    }

    @Test
    void validate_validGeneralInfo_noException() {
        assertDoesNotThrow(() -> validator.validate(validGeneralInfo()));
    }

    @Test
    void validate_nullGeneralInfo_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("General information cannot be null"));
    }

    @Test
    void validate_multipleInvalidFields_reportsAllErrors() {
        IntegrationDraftRequest.GeneralInformation info = new IntegrationDraftRequest.GeneralInformation();
        info.setProvider(""); // empty
        info.setIntegrationId("A".repeat(51)); // too long
        info.setIntegrationOwner("Owner@Name"); // invalid chars

        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Provider is mandatory"));
        assertTrue(msg.contains("Integration ID is mandatory"));
        assertTrue(msg.contains("Integration owner is mandatory"));
    }

    @Test
    void validate_emptyProvider_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setProvider("");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Provider is mandatory"));
    }

    @Test
    void validate_overlengthProvider_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setProvider("A".repeat(51));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Provider is mandatory"));
    }

    @Test
    void validate_invalidProviderPattern_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setProvider("Provider@123");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Provider is mandatory"));
    }

    @Test
    void validate_emptyIntegrationId_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationId("");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration ID is mandatory"));
    }

    @Test
    void validate_overlengthIntegrationId_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationId("B".repeat(51));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration ID is mandatory"));
    }

    @Test
    void validate_invalidIntegrationIdPattern_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationId("Integration 123"); // contains space
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration ID is mandatory"));
    }

    @Test
    void validate_emptyIntegrationOwner_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationOwner("");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration owner is mandatory"));
    }

    @Test
    void validate_overlengthIntegrationOwner_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationOwner("C".repeat(51));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration owner is mandatory"));
    }

    @Test
    void validate_invalidIntegrationOwnerPattern_throwsException() {
        IntegrationDraftRequest.GeneralInformation info = validGeneralInfo();
        info.setIntegrationOwner("Owner@Name");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(info));
        assertTrue(ex.getMessage().contains("Integration owner is mandatory"));
    }
}
