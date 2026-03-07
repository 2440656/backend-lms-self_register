package com.cognizant.lms.userservice.dto;

import lombok.Data;

@Data
public class UserSettingsRequestDto {
    private String tenantCode;
    private String type;
    private String voiceId;
    private String theme;
    private String emailId;
    private String userId;
}