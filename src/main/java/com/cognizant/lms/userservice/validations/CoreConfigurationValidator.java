package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CoreConfigurationValidator {

    private static final int MAX_LENGTH = 250;
    private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";

    public void validate(IntegrationDraftRequest.CoreConfiguration coreConfig) {
        List<String> errors = new ArrayList<>();

        if (coreConfig == null) {
            errors.add("Core configuration cannot be null.");
        } else {
            validateField(coreConfig.getHostName(), "Host Name", errors);
            validateField(coreConfig.getClientId(), "Client ID", errors);
            validateField(coreConfig.getOrganizationId(), "Organization ID", errors);
            validateField(coreConfig.getClientSecret(), "Client Secret", errors);

            if (coreConfig.getFields() != null) {
                for (IntegrationDraftRequest.FieldEntry entry : coreConfig.getFields()) {
                    validateField(entry.getFieldValue(), entry.getFieldName(), errors);
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMsg = String.join(" ", errors);
            log.error("CoreConfiguration validation failed: {}", errorMsg);
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
        if (!value.matches(ALPHANUMERIC_REGEX)) {
            errors.add(fieldName + " must be alphanumeric only.");
        }
    }
}
