package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingsRequest {
  private String reviewEmail;
  private String courseReviewCommentType;

}
