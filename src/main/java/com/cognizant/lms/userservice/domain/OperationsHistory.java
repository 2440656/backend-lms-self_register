package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationsHistory {

  private String pk;
  private String sk;
  private String fileName;
  private String uploadedBy;
  private String createdOn;
  private String email;
  private String tenantCode;
  private String operation;
  private String area;
  private String uploadStatus;
  private String uploadedFileS3key;
  private String fileStatus;
  private String fileProgress;


  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  @DynamoDbSecondaryPartitionKey(
      indexNames = "gsi_sort_${AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY}")
  public String getCreatedOn() {
    return createdOn;
  }
}
