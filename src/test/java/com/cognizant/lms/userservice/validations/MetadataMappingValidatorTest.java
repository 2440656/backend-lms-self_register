package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataMappingValidatorTest {

    private MetadataMappingValidator validator;

    @BeforeEach
    void setup() {
        validator = new MetadataMappingValidator();
    }

    @Test
    void validate_validMetaData_shouldPass() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("ValidPrefix");
        metaData.setMetaDataMappings(List.of(
                new IntegrationDraftRequest.MetaDataMappings("ThirdPartyField1", "SkillSpringField1")
        ));
        assertDoesNotThrow(() -> validator.validate(metaData));
    }

    @Test
    void validate_nullMetaData_shouldThrow() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("Metadata cannot be null"));
    }

    @Test
    void validate_invalidPrefix_shouldThrow() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("Invalid@Prefix");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(metaData));
        assertTrue(ex.getMessage().contains("Prefix must be alphanumeric only"));
    }

    @Test
    void validate_prefixExceedsMaxLength_shouldThrow() {
        String longPrefix = "a".repeat(51);
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix(longPrefix);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(metaData));
        assertTrue(ex.getMessage().contains("Prefix must not exceed 50 characters"));
    }

    @Test
    void validate_invalidThirdPartyMetadataField_shouldThrow() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("ValidPrefix");
        metaData.setMetaDataMappings(List.of(
                new IntegrationDraftRequest.MetaDataMappings("Invalid@Field", "SkillSpringField1")
        ));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(metaData));
        assertTrue(ex.getMessage().contains("Third Party Metadata Field must be alphanumeric only"));
    }

    @Test
    void validate_invalidSkillSpringMetadataField_shouldThrow() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("ValidPrefix");
        metaData.setMetaDataMappings(List.of(
                new IntegrationDraftRequest.MetaDataMappings("ThirdPartyField1", "SkillSpring@Field")
        ));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(metaData));
        assertTrue(ex.getMessage().contains("SkillSpring Metadata Field must be alphanumeric only"));
    }

    @Test
    void validate_emptyFields_shouldThrow() {
        IntegrationDraftRequest.MetaData metaData = new IntegrationDraftRequest.MetaData();
        metaData.setPrefix("");
        metaData.setMetaDataMappings(List.of(
                new IntegrationDraftRequest.MetaDataMappings("", "")
        ));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(metaData));
        assertTrue(ex.getMessage().contains("Third Party Metadata Field is mandatory"));
        assertTrue(ex.getMessage().contains("SkillSpring Metadata Field is mandatory"));
    }
}
