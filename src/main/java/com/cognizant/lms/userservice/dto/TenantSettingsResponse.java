package com.cognizant.lms.userservice.dto;

import com.cognizant.lms.userservice.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingsResponse {

  private String type;
  private String reviewEmail;
  private String courseReviewCommentType;
  private String settingName;
  private String createdOn;
  private String createdBy;
  private String updatedDate;
  private String updatedBy;

}
