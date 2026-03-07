package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserEmailDto {
    private String emailId;
    private String userId;
}

