package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AspirationalRoleDto {
    private String id;
    private String name;
    private String type; // USERROLE, INTEREST, ROLE
}
