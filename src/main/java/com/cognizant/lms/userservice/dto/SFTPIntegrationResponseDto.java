package com.cognizant.lms.userservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SFTPIntegrationResponseDto {
  private String pk;
  private String sk;
  private String pageName;
  private String lmsIntegrationId;
  private String integrationType;
  private String status;
  private String type;
  private String updatedDate;

  // configuration details
  private String provider;
  private String syncType;
  private String location;
  private String userName;
  private String password;
  private String host;
  private String port;
  private String testConnection;
  //settings
  private String syncTime;

  //category-mapping
  private String thirdPartyCategoryType;
  private String skillSpringCategoryType;
  private List<SftpCategoryTypeMapping> sftpCategoryTypeMapping;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SftpCategoryTypeMapping {
    private String thirdPartyCategoryType;
    private String skillSpringCategoryType;
  }


}
