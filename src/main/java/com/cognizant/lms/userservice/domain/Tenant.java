package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/**
 * Represents a Tenant and Integration entity for DynamoDB.
 */
@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {
  private String pk;
  private String sk;
  private String type;
  private String name;
  private String idpPreferences;
  private String settings;
  private String createdOn;
  private String status;
  private String tenantIdentifier;
  private String updatedBy;
  private String createdBy;
  private String reviewEmail;
  private String courseReviewCommentType;
  private String updatedDate;
  private String settingName;
  private String category;
  private String message;
  private Boolean messageStatus;

  // Integration fields
  private String lmsIntegrationId;
  private String provider;
  private String integrationType;
  private String integrationId;
  private String integrationOwner;
  private String hostName;
  private String clientId;
  private String clientSecret;
  private String organizationId;
  private String fieldName;
  private String fieldValue;

    // --- Fields shared by PopularLink and QuickLink entities ---
  private String linkId;
  private String title;
  private String url;
  private String description;
  private String action;
  private String modifiedOn;
  private String modifiedBy;
  private Integer index;
  private String iconKey;
  private String iconFileName;

  // -- field specifically for BannerManagement
    private String bannerId;
    private String bannerTitle;
    private String bannerDescription;
    private String bannerStatus;
    private String startDate;
    private String endDate;
    private String bannerHeading;
    private String bannerSubHeading;
    private String bannerRedirectionUrl;
    private String bannerImageKey;

  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  @DynamoDbSortKey
  @DynamoDbSecondarySortKey(indexNames = "gsi_type")
  public String getSk() {
    return sk;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = "gsi_type")
  public String getType() {
    return type;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = "gsi_banner_type")
  public String getpk() {
    return pk;
  }

  @DynamoDbSecondarySortKey(indexNames = "gsi_banner_type")
  public String getStartDate() {
    return startDate;
  }
}

