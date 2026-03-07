package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContentMappingValidatorTest {

    private ContentMappingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContentMappingValidator();
    }

    @Test
    void validate_validContentMapping_shouldPass() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCategoryMappingType("assignSame");
        mapping.setCategoryName("Category1");
        mapping.setContentTypeMapping(List.of(
                new IntegrationDraftRequest.ContentTypeMapping("Video1", "SkillSpringVideo1")
        ));
        mapping.setCategoryTypeMapping(List.of(
                new IntegrationDraftRequest.CategoryTypeMapping("TypeA", "TypeB")
        ));
        mapping.setCompletionSyncMapping(List.of(
                new IntegrationDraftRequest.CompletionSyncMapping("Completed", "Done")
        ));

        assertDoesNotThrow(() -> validator.validate(mapping));
    }

    @Test
    void validate_nullContentMapping_shouldThrow() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("Content mapping cannot be null"));
    }

    @Test
    void validate_invalidCategoryMappingType_shouldThrow() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCategoryMappingType("!@#");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(mapping));
        assertTrue(ex.getMessage().contains("Category Mapping Type must be alphanumeric only"));
    }

    @Test
    void validate_assignSameCategoryNameInvalid_shouldThrow() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCategoryMappingType("assignSame");
        mapping.setCategoryName("Invalid Name!");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(mapping));
        assertTrue(ex.getMessage().contains("Category Name must be alphanumeric only"));
    }

    @Test
    void validate_fieldExceedsMaxLength_shouldThrow() {
        String longString = "a".repeat(51);
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCategoryMappingType(longString);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(mapping));
        assertTrue(ex.getMessage().contains("must not exceed 50 characters"));
    }

    @Test
    void validate_completionSyncMapping_valid_shouldPass() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCompletionSyncMapping(List.of(
                new IntegrationDraftRequest.CompletionSyncMapping("Completed", "Done")
        ));
        mapping.setCategoryMappingType("assignSame");
        mapping.setCategoryName("Category1");
        assertDoesNotThrow(() -> validator.validate(mapping));
    }

    @Test
    void validate_contentTypeMapping_valid_shouldPass() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setContentTypeMapping(List.of(
                new IntegrationDraftRequest.ContentTypeMapping("Video1", "SkillSpringVideo1")
        ));
        mapping.setCategoryMappingType("assignSame");
        mapping.setCategoryName("Category1");
        assertDoesNotThrow(() -> validator.validate(mapping));
    }

    @Test
    void validate_categoryTypeMapping_valid_shouldPass() {
        IntegrationDraftRequest.ContentMapping mapping = new IntegrationDraftRequest.ContentMapping();
        mapping.setCategoryTypeMapping(List.of(
                new IntegrationDraftRequest.CategoryTypeMapping("TypeA", "TypeB")
        ));
        mapping.setCategoryMappingType("assignSame");
        mapping.setCategoryName("Category1");
        assertDoesNotThrow(() -> validator.validate(mapping));
    }

}
