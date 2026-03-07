package com.cognizant.lms.userservice.dto;

import lombok.Data;

@Data
public class TermsAndUseDTO {
    private String tenantCode;
    private String termsAndUseContent;
    private boolean enableTermsAndUse;
    private String createdOn;
}
