package com.cognizant.lms.userservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillLookups {
  private String pk;
  private String sk;
  private String type;
  private String name;
  private String tenantType;
  private String normalizedName;
  private String normalizedCode;
  private String active;
  private String gsiTypeSk;
  private String effectiveDate;
  private String skillCode;
  private String skillName;
  private String skillDescription;
  private String skillType;
  private String status;
  private String skillCategory;
  private String skillSubCategory;

  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  @DynamoDbSortKey
  public String getSk() {
    return sk;
  }
}
