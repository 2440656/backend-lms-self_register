package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;

@Slf4j
@Component
public class S3Util {

  private S3Client s3ThumbnailClient;
  private S3Presigner s3Presigner;

  public S3Util(S3Client s3ThumbnailClient, S3Presigner s3Presigner) {
    this.s3ThumbnailClient = s3ThumbnailClient;
    this.s3Presigner = s3Presigner;
  }

  public boolean saveFileToS3(MultipartFile file, String bucketName, String key) {
    try {
      String originalFileName = file.getOriginalFilename();
      s3ThumbnailClient.putObject(PutObjectRequest
              .builder()
              .bucket(bucketName)
              .key(key)
              .build(),
          RequestBody.fromBytes(file.getBytes()));
      log.info("File uploaded to S3: {}", originalFileName);
      return confirmFileUploadToS3(originalFileName, bucketName, key);
    } catch (S3Exception | IOException e) {
      log.error("Error uploading file to S3: {}", e.getMessage());
      return false;
    }
  }

  public boolean confirmFileUploadToS3(String originalFileName, String bucketName,
                                       String key) {
    try {
      s3ThumbnailClient.getObject(GetObjectRequest
          .builder()
          .bucket(bucketName)
          .key(key)
          .build());
      log.info("File is Present in S3: {}", originalFileName);
      return true;
    } catch (S3Exception e) {
      log.error("File is not present in S3: {}", e.awsErrorDetails().errorMessage());
      return false;
    }
  }



  public Resource downloadFileFromS3(String filename, String fileType, String bucketName, boolean isSkillFile)
          throws Exception {
    try {
      String fileLocation;
      String tenantCode = TenantUtil.getTenantCode();
      if (isSkillFile) {
          // Use paths specific to skills
          if (Constants.FILE_TYPE_TXT.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.SKILLS_ERROR_LOG_PREFIX;
          } else if (Constants.FILE_TYPE_CSV.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.S3_SKILLS_MASTERDATA_PREFIX;
          } else {
              throw new IllegalArgumentException("Unsupported file type for skills: " + fileType);
          }
      } else {
          // Use paths specific to users
          if (Constants.FILE_TYPE_TXT.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.ERROR_LOG_PREFIX;
          } else if (Constants.FILE_TYPE_CSV.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.S3_PREFIX;
          } else if (Constants.FILE_TYPE_TEMPLATE.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.TEMPLATE_PATH;
          } else if (Constants.FILE_TYPE_IMAGE.equalsIgnoreCase(fileType)) {
              fileLocation = tenantCode + Constants.IMAGE_PATH;
          } else {
              throw new IllegalArgumentException("Unsupported file type: " + fileType);
          }
      }

      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
              .bucket(bucketName)
              .key(fileLocation + filename)
              .build();
      InputStream inputStream =
          s3ThumbnailClient.getObject(getObjectRequest, ResponseTransformer.toInputStream());
      return new
              InputStreamResource(inputStream);
    } catch (S3Exception e) {
      log.error("Error downloading file from S3: {}", e.awsErrorDetails().errorMessage());
      throw new IOException("Error downloading file from S3", e);
    }
  }

  public Resource downloadFileFromS3ForDeactivateUser(String filename, String fileType, String bucketName,String tenantCode)
          throws Exception {
    try {
      String fileLocation;

      if (Constants.FILE_TYPE_TXT.equalsIgnoreCase(fileType)) {
        fileLocation = tenantCode + Constants.ERROR_LOG_PREFIX;
      } else if (Constants.FILE_TYPE_CSV.equalsIgnoreCase(fileType)) {
        fileLocation = tenantCode + Constants.S3_PREFIX;
      } else if (Constants.FILE_TYPE_TEMPLATE.equalsIgnoreCase(fileType)) {
        fileLocation = tenantCode + Constants.TEMPLATE_PATH;
      } else if (Constants.FILE_TYPE_IMAGE.equalsIgnoreCase(fileType)) {
        fileLocation = tenantCode + Constants.IMAGE_PATH;
      } else {
        throw new
                IllegalArgumentException("Unsupported file type: " + fileType);
      }

      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
              .bucket(bucketName)
              .key(fileLocation + filename)
              .build();
      InputStream inputStream =
          s3ThumbnailClient.getObject(getObjectRequest, ResponseTransformer.toInputStream());
      return new
              InputStreamResource(inputStream);
    } catch (S3Exception e) {
      log.error("Error downloading file from S3 for Deactivate User: {}", e.awsErrorDetails().errorMessage());
      throw new IOException("Error downloading file from S3 for Deativate User", e);
    }
  }

  public void uploadFileToS3(String bucketName, String key, byte[] fileBytes, String contentType) {
    try {
      log.info("Uploading file to S3 bucket: {} with key: {}", bucketName, key);
      
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .contentType(contentType)
          .build();
      
      s3ThumbnailClient.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
      
      log.info("File uploaded successfully to S3: {}", key);
    } catch (S3Exception e) {
      log.error("Error uploading file to S3: {}", e.getMessage());
      throw new RuntimeException("Failed to upload file to S3", e);
    }
  }

  public String generatePresignedUrl(String bucketName, String key, int expirationMinutes) {
    try {
      log.info("Generating presigned URL for bucket: {} with key: {}", bucketName, key);
      
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build();
      
      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(expirationMinutes))
          .getObjectRequest(getObjectRequest)
          .build();
      
      PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
      String presignedUrl = presignedRequest.url().toString();
      
      log.info("Generated presigned URL successfully for key: {}", key);
      return presignedUrl;
    } catch (S3Exception e) {
      log.error("Error generating presigned URL: {}", e.getMessage());
      throw new RuntimeException("Failed to generate presigned URL", e);
    }
  }

  public void deleteFileFromS3(String bucketName, String key) {
    try {
      log.info("Deleting file from S3 bucket: {} with key: {}", bucketName, key);
      
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build();
      
      s3ThumbnailClient.deleteObject(deleteObjectRequest);
      
      log.info("File deleted successfully from S3: {}", key);
    } catch (S3Exception e) {
      log.error("Error deleting file from S3: {}", e.getMessage());
      throw new RuntimeException("Failed to delete file from S3", e);
    }
  }
}
