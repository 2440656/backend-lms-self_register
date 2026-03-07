package com.cognizant.lms.userservice.config;

import com.cognizant.lms.userservice.constants.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

@Configuration
public class SqsConfig {
  private final String awsRegion;
  private final String appEnv;
  private final String roleArn;

  public SqsConfig(@Value("${REGION_NAME}") String awsRegion,
                   @Value("${APP_ENV}") String appEnv,
                   @Value("${ROLE_ARN}") String roleArn) {
    this.awsRegion = awsRegion;
    this.appEnv = appEnv;
    this.roleArn = roleArn;
  }

  @Bean
  public SqsClient sqsClient() {
    if (!appEnv.equalsIgnoreCase(Constants.appEnv) && !appEnv.equalsIgnoreCase(Constants.SbxEnv)) {
      StsClient stsClient = StsClient.builder()
          .region(Region.of(awsRegion))
          .build();

      StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider
          = StsAssumeRoleCredentialsProvider.builder()
          .refreshRequest(r -> r.roleArn(roleArn).roleSessionName("SQSAccessSession"))
          .stsClient(stsClient)
          .build();

      return SqsClient.builder()
          .credentialsProvider(stsAssumeRoleCredentialsProvider)
          .region(Region.of(awsRegion))
          .build();
    }
    return SqsClient.builder()
        .region(Region.of(awsRegion))
        .build();
  }
}
