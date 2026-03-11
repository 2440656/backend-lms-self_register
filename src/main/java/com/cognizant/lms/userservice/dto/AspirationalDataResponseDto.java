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
public class AspirationalDataResponseDto {
    private List<AspirationalRoleDto> userRoles;
    private List<AspirationalRoleDto> interests;
    private List<AspirationalRoleDto> roles;
}
