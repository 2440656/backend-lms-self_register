package com.cognizant.lms.userservice.config;

import static com.cognizant.lms.userservice.constants.Constants.COGNITO_TIMEOUT;
import static com.cognizant.lms.userservice.constants.Constants.MAX_CONCURRENCY;

import com.cognizant.lms.userservice.constants.Constants;
import java.time.Duration;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

@Configuration
public class CognitoAsyncConfig {

  private final String awsRegion;
  private final String appEnv;
  private final String roleArn;

  public CognitoAsyncConfig(@Value("${REGION_NAME}") String awsRegion,
                            @Value("${APP_ENV}") String appEnv,
                            @Value("${ROLE_ARN}") String roleArn) {
    this.awsRegion = awsRegion;
    this.appEnv = appEnv;
    this.roleArn = roleArn;
  }

//  @Bean
//  public StsClient stsClient() {
//    return StsClient.builder()
//        .region(Region.of(awsRegion))
//        .build();
//  }

  @Bean
  public CognitoIdentityProviderAsyncClient cognitoAsyncClient() {
    SdkAsyncHttpClient nettyClient = NettyNioAsyncHttpClient.builder()
        .maxConcurrency(MAX_CONCURRENCY)
        .connectionAcquisitionTimeout(Duration.ofSeconds(COGNITO_TIMEOUT))
        .build();

    URI cognitoHttpsEndpoint = URI.create(String.format("https://cognito-idp.%s.amazonaws.com", awsRegion));


    if (!appEnv.equalsIgnoreCase(Constants.appEnv) && !appEnv.equalsIgnoreCase(Constants.SbxEnv)) {
      StsClient stsClient = StsClient.builder()
          .region(Region.of(awsRegion))
          .build();

      StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider
          = StsAssumeRoleCredentialsProvider.builder()
          .refreshRequest(r -> r.roleArn(roleArn).roleSessionName("CognitoAccessSession"))
          .stsClient(stsClient)
          .build();

      return CognitoIdentityProviderAsyncClient.builder()
          .credentialsProvider(stsAssumeRoleCredentialsProvider)
          .region(Region.of(awsRegion))
          .endpointOverride(cognitoHttpsEndpoint) // enforce HTTPS endpoint
          .httpClient(nettyClient)
          .build();
    }
    return CognitoIdentityProviderAsyncClient.builder()
        .region(Region.of(awsRegion))
        .endpointOverride(cognitoHttpsEndpoint) // enforce HTTPS endpoint
        .httpClient(nettyClient)
        .build();
  }

  @Bean
  public SqsClient sqsClientBuilder() {

    URI sqsHttpsEndpoint = URI.create(String.format("https://sqs.%s.amazonaws.com", awsRegion));

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
          .endpointOverride(sqsHttpsEndpoint) // enforce HTTPS endpoint
          .build();
    }
    return SqsClient.builder()
        .region(Region.of(awsRegion))
        .endpointOverride(sqsHttpsEndpoint) // enforce HTTPS endpoint
        .build();
  }
}