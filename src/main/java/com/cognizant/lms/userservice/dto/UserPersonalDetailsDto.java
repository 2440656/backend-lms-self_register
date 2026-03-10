package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPersonalDetailsDto {
  private String userId;
  private String firstName;
  private String lastName;
  private String emailAddress;
  private String country;
  private String institutionName;
  private String currentRole;
}
