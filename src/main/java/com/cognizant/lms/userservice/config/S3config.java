package com.cognizant.lms.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3config {
  private final String awsRegion;
  private final String thumbnailBucketRegion;

  public S3config(@Value("${REGION_NAME}") String awsRegion, @Value("${THUMBNAIL_BUCKET_REGION_NAME}") String thumbnailBucketRegion) {
      this.awsRegion = awsRegion;
      this.thumbnailBucketRegion = thumbnailBucketRegion;
  }

  @Bean
  public S3Client s3ThumbnailClient() {
      return S3Client.builder()
              .region(Region.of(thumbnailBucketRegion))
              .build();
  }

    @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .region(Region.of(awsRegion))
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .region(Region.of(awsRegion))
        .build();
  }
}
