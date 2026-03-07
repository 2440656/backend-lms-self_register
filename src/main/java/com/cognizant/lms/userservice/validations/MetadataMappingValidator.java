package com.cognizant.lms.userservice.validations;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MetadataMappingValidator {

    private static final int MAX_LENGTH = 50;
    private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";

    public void validate(IntegrationDraftRequest.MetaData metaData) {
        List<String> errors = new ArrayList<>();

        if(metaData == null) {
            errors.add("Metadata cannot be null.");
        } else {
            if(metaData.getPrefix() != null && !metaData.getPrefix().trim().isEmpty()){
                validateField(metaData.getPrefix(), "Prefix", ALPHANUMERIC_REGEX, errors);
            }
            if(metaData.getMetaDataMappings() != null){
                for (IntegrationDraftRequest.MetaDataMappings metaDataMappings : metaData.getMetaDataMappings()) {
                    validateField(metaDataMappings.getThirdPartyMetadataField(), "Third Party Metadata Field", ALPHANUMERIC_REGEX, errors);
                    validateField(metaDataMappings.getSkillSpringMetadataField(), "SkillSpring Metadata Field", ALPHANUMERIC_REGEX, errors);
                }
            }
            if(metaData.getLessonMetaDataMappings()!=null)
            {
                for (IntegrationDraftRequest.LessonMetaDataMappings lessonMetadataMappings : metaData.getLessonMetaDataMappings()) {
                    validateField(lessonMetadataMappings.getThirdPartyLessonMetadataField(), "Third Party Lesson Field", ALPHANUMERIC_REGEX, errors);
                    validateField(lessonMetadataMappings.getSkillSpringLessonMetadataField(), "SkillSpring Lesson Field", ALPHANUMERIC_REGEX, errors);
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
