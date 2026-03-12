package com.cognizant.lms.userservice.domain;

import com.cognizant.lms.userservice.dto.TenantDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

  private String pk;
  private String sk;
  private String firstName;
  private String lastName;
  private String gsiSortFNLN;
  private String name;
  private String institutionName;
  private String userType;
  private String role;
  private String status;
  private String userAccountExpiryDate;
  private String emailId;
  private String type;
  private String createdOn;
  private String modifiedOn;
  private String modifiedBy;
  private String createdBy;
  private String menteeId;
  private String mentorId;
  private String tenantCode;
  private String lastLoginTimestamp;
  private String viewOnlyAssignedCourses;
  private String loginOption;
  private String passwordChangedDate;
  private String reactivatedDate;
  private String portal;

  private String country; //Added country field
  private String termsAccepted;
  private String termsAcceptedDate;
  private String preferredUI;
  private String isWatchedTutorial;
  private String tutorialWatchDate;
  private String videoLaunchCount;

  // Profile fields (separate from existing role field used for system permissions)
  private String currentRole; // User's self-selected current role (e.g., "Developer", "Designer")

  // Aspirational profile fields
  private String selectedUserRole;
  private String selectedInterests; // Comma-separated list
  private String selectedRoles; // Comma-separated list

  // Profile photo field
  private String profilePhotoUrl; // S3 URL of the user's profile photo

  private TenantDTO tenant;

  @DynamoDbIgnore
  public TenantDTO getTenant() {
    return tenant;
  }

  @DynamoDbIgnore
  public void setTenant(TenantDTO tenant) {
    this.tenant = tenant;
  }

  public User(String firstName, String lastName, String institutionName, String emailId,
              String userType, String role, String userAccountExpiryDate, String viewOnlyAssignedCourses, String loginOption) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.institutionName = institutionName;
    this.emailId = emailId;
    this.userType = userType;
    this.role = role;
    this.userAccountExpiryDate = userAccountExpiryDate;
    this.viewOnlyAssignedCourses = viewOnlyAssignedCourses;
    this.loginOption = loginOption;
  }

  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  @DynamoDbSecondaryPartitionKey(
      indexNames = "gsi_sort_${AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY}")
  public String getCreatedOn() {
    return createdOn;
  }

}
