package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class UserActivityLogDto {
    private String pk;
    private String sk;
    private String userId;
    private String firstName;
    private String lastName;
    private String emailId;
    private String ipAddress;
    private String deviceDetails;
    private String activityType;
    private String status;
    private String timestamp;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi_token_iat")
    public String getTimestamp() {
        return timestamp;
    }
}
