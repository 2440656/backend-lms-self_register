package com.cognizant.lms.userservice.dto;

import lombok.Data;

@Data
public class UserSettingsRequest {
    private String type;
    private String option;
}