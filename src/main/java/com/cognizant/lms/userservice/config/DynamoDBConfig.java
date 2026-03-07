package com.cognizant.lms.userservice.config;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Slf4j
public class DynamoDBConfig {
  private String dynamoDbEndpoint;
  private String awsRegion;

  public DynamoDBConfig(@Value("${AWS_DYNAMODB_ENDPOINT}") String dynamoDbEndpoint,
                        @Value("${REGION_NAME}") String awsRegion) {
    this.awsRegion = awsRegion;
    this.dynamoDbEndpoint = dynamoDbEndpoint;
  }

  @Bean
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(dynamoDbEndpoint))
        .region(Region.of(awsRegion))
        .build();
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
  }

  @Bean
  public DynamoDbEnhancedClient getDynamoDBEnhancedClient() {
    return dynamoDbEnhancedClient(dynamoDbClient());
  }
}
