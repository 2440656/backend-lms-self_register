package com.cognizant.lms.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AspirationalRole {

    private String pk;
    private String sk;
    private String userRoleName;
    private String interestName;
    private String roleName;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() {
        return sk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }
}