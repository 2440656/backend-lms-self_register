package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserHomeProfileDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String profilePhotoUrl;
}
