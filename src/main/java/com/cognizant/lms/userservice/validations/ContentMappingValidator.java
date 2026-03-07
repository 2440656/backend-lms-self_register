package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ContentMappingValidator {

    private static final int MAX_LENGTH = 50;
    private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";
    private static final String ALPHANUMERIC_WITH_SPACES_REGEX = "^[a-zA-Z0-9\\s]+$";

    public void validate(IntegrationDraftRequest.ContentMapping contentMapping) {
        List<String> errors = new ArrayList<>();

        if (contentMapping == null) {
            errors.add("Content mapping cannot be null.");
        } else {
            validateField(contentMapping.getCategoryMappingType(), "Category Mapping Type", ALPHANUMERIC_REGEX, errors);
            if(contentMapping.getCategoryMappingType().equalsIgnoreCase("assignSame")) {
                validateField(contentMapping.getCategoryName(), "Category Name", ALPHANUMERIC_WITH_SPACES_REGEX, errors);
            }

            if(contentMapping.getContentTypeMapping() != null) {
                for (IntegrationDraftRequest.ContentTypeMapping contentTypeMapping : contentMapping.getContentTypeMapping()) {
                    validateField(contentTypeMapping.getThirdPartyContentType(), contentTypeMapping.getSkillSpringContentType(), ALPHANUMERIC_REGEX, errors);
                }
            }

            if(contentMapping.getCategoryTypeMapping() != null) {
                for (IntegrationDraftRequest.CategoryTypeMapping categoryTypeMapping : contentMapping.getCategoryTypeMapping()) {
                    validateField(categoryTypeMapping.getThirdPartyCategoryType(), categoryTypeMapping.getSkillSpringCategoryType(), ALPHANUMERIC_REGEX, errors);
                }
            }

            if(contentMapping.getCompletionSyncMapping() != null) {
                for (IntegrationDraftRequest.CompletionSyncMapping completionSyncMapping : contentMapping.getCompletionSyncMapping()) {
                    validateField(completionSyncMapping.getThirdPartyCompletionStatus(), completionSyncMapping.getSkillSpringCompletionStatus(), ALPHANUMERIC_REGEX, errors);
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMsg = String.join(" ", errors);
            log.error("ContentMapping validation failed: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private void validateField(String value, String fieldName, String regex, List<String> errors) {
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
