package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSettings {
    private String pk;
    private String sk;
    private String tenantCode;
    private String type;
    private String voiceId;
    private String theme;
    private String createdBy;
    private String createdDate;
    private String updatedBy;
    private String updatedDate;
    private String emailId;

    @DynamoDbPartitionKey
    public String getPk() { return pk; }

    @DynamoDbSortKey
    public String getSk() { return sk; }
}