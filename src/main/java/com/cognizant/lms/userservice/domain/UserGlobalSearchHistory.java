package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGlobalSearchHistory {
  private String pk;
  private String sk;
  private String type;
  private String keywordNormal;
  private String keywordOriginal;
  private String createdAt;
  private String modifiedAt;
  private String active;
  private String gsiPk;
  private String gsiSk;


  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }


}
