package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GeneralInformationValidator {

    public void validate(IntegrationDraftRequest.GeneralInformation generalInfo) {
        List<String> errors = new ArrayList<>();

        if (generalInfo == null) {
            errors.add("General information cannot be null.");
        } else {
            if (generalInfo.getProvider() == null || generalInfo.getProvider().trim().isEmpty()
                    || generalInfo.getProvider().length() > 50
                    || !generalInfo.getProvider().matches("^[a-zA-Z0-9 ]+$")) {
                errors.add("Provider is mandatory, max 50 chars, alphanumeric and spaces only.");
            }
            if (generalInfo.getIntegrationId() == null || generalInfo.getIntegrationId().trim().isEmpty()
                    || generalInfo.getIntegrationId().length() > 50
                    || !generalInfo.getIntegrationId().matches("^[a-zA-Z0-9]+$")) {
                errors.add("Integration ID is mandatory, max 50 chars, alphanumeric only.");
            }
            if (generalInfo.getIntegrationOwner() == null || generalInfo.getIntegrationOwner().trim().isEmpty()
                    || generalInfo.getIntegrationOwner().length() > 50
                    || !generalInfo.getIntegrationOwner().matches("^[a-zA-Z0-9 ]+$")) {
                errors.add("Integration owner is mandatory, max 50 chars, alphanumeric and spaces only.");
            }
        }

        if (!errors.isEmpty()) {
            String errorMsg = String.join(" ", errors);
            log.error("GeneralInformation validation failed: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }
}