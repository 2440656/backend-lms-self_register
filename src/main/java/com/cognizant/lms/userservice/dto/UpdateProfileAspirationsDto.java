package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileAspirationsDto {
    private String selectedUserRole;
    private List<String> selectedInterests;
    private List<String> selectedRoles;
}
