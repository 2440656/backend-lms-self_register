package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateUserRequest {
  private String firstName;
  private String lastName;
  private String institutionName;
  private String userAccountExpiryDate;
  private String role;
  private String userType;
  private String status;
  private String country; //Added country field
}
