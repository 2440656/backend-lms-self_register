package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAuditLogDto {
    private String pk;
    private String sk;
    private String emailId;
    private String name;
    private String action;
    private String institutionName;
    private String modifiedFields;
    private String actionPerformedBy;
    private String actionTimestamp;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

}
