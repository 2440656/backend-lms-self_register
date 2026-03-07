package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDateDTO {
    private String emailId;
    private String lastLoginTimestamp;
    private String passwordChangedDate;
    private String termsAccepted;
    private String termsAcceptedDate;
}
