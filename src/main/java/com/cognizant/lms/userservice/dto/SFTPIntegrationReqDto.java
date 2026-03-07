package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SFTPIntegrationReqDto {

  private String pageName;
  private String lmsIntegrationId;
  private String integrationType;
  private String action;
  private String status;
  private Configuration configuration;
  private SftpSettingsDTO sftpSettingsDTO;
  private CategoryMapping categoryMapping;
  private String uniqIntegrationKey;
  private String versionStatus;
  private String reasonForChange;
  private boolean edit;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Configuration {
    private String provider;
    private String syncType;
    private String location;
    private String userName;
    private String password;
    private String host;
    private String port;
    private String testConnection;

  }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SftpSettingsDTO {
        private String syncType;
        private String syncSchedule;
        private String weekDay;
        private String syncTime;
        private String learnerGroups;
        private String adminGroups;
    }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CategoryMapping{
    private List<SftpCategoryTypeMapping> categoryTypeMappings;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SftpCategoryTypeMapping{
    private String thirdPartyCategoryType;
    private String skillSpringCategoryType;
  }
}
