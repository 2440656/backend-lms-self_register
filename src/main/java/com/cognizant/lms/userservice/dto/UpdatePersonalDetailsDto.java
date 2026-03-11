package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePersonalDetailsDto {
    private String firstName;
    private String lastName;
    private String country;
    private String institutionName;
    private String currentRole;
}
