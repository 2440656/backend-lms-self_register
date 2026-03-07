package com.cognizant.lms.userservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoggedInUser {
  private String userId;
  private String name;
  private String firstName;
  private String lastName;
  private String userEmail;
  private List<String> userRoles;
  private boolean isFirstLogin;
  private String viewOnlyAssignedCourses;
  private String loginOption;
  private String termsAccepted;
  private String termsAcceptedDate;
  private String userType;
  private String tenantCode;
  private String portal;
  private String preferredUI;
  private TenantConfigDto tenantConfig;
  private String isWatchedTutorial;
  private  String videoLaunchCount;
  private String tutorialWatchDate;
  private TermsAndUseDTO termsAndUse;
  private String country;
}
