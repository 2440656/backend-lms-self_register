package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Represents a Role entity for DynamoDB.
 */
@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
  private String pk;
  private String sk;
  private String type;
  private String active;
  private String description;
  private String name;

  @DynamoDbPartitionKey
  public String getPK() {
    return pk;
  }

}
