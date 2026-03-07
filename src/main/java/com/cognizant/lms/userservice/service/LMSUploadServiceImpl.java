package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.S3config;
import com.cognizant.lms.userservice.dto.PresignUploadResponseDTO;
import com.cognizant.lms.userservice.dto.ResourceJsonUploadDTO;
import com.cognizant.lms.userservice.dto.ResourceJsonUploadResponseDTO;
import com.cognizant.lms.userservice.dto.StartUploadRequest;
import com.cognizant.lms.userservice.dto.UploadFileRequest;
import com.cognizant.lms.userservice.utils.BucketPath;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class LMSUploadServiceImpl implements LMSUploadService {

  public static final String TEMPLATES = "templates";
  private static final Logger log = LoggerFactory.getLogger(LMSUploadServiceImpl.class);

  private final String bucketName;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  public LMSUploadServiceImpl(@Value("${AWS_S3_BUCKET_NAME}") String bucketName,
                              S3config s3config) {
    this.s3Client = s3config.s3Client();
    this.bucketName = bucketName;
    this.s3Presigner = s3config.s3Presigner();
  }

  @Override
  public String uploadFile(UploadFileRequest request) {
    try {
      String fileKey = request.getUniqueId()+"/"+ request.getFileName();
      log.info("Uploading file to S3: {}", fileKey);
      PutObjectRequest uploadPartRequest = PutObjectRequest
          .builder()
          .bucket(bucketName)
          .key(fileKey)
          .build();
      RequestBody requestBody = RequestBody.fromBytes(request.getFile().getBytes());
      s3Client.putObject(uploadPartRequest, requestBody);
      return fileKey;
    } catch (Exception e) {
      log.error("Exception occurred while uploading file: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public byte[] downloadZipFileFromS3(String uniqueId) throws IOException {
    ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build();
    String path = uniqueId;
    ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
    List<S3Object> objects = listObjectsResponse.contents();
    List<byte[]> files = new ArrayList<>();
    List<String> fileNames = new ArrayList<>();
    log.info(path);
    for (S3Object object : objects) {
      String key = object.key();
      if (key.startsWith(path)) {
        log.info("Downloading file from S3: {}", key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();
        ResponseBytes<GetObjectResponse> s3ObjectBytes = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        files.add(s3ObjectBytes.asByteArray());
        fileNames.add(key.substring(key.lastIndexOf('/') + 1));
      }
    }


    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

    for (int i = 0; i < files.size(); i++) {
      ZipEntry zipEntry = new ZipEntry(fileNames.get(i));
      zipOutputStream.putNextEntry(zipEntry);
      zipOutputStream.write(files.get(i));
      zipOutputStream.closeEntry();
    }

    zipOutputStream.finish();
    zipOutputStream.close();
    byteArrayOutputStream.flush();
    byteArrayOutputStream.close();
    byte[] zipBytes = byteArrayOutputStream.toByteArray();
    log.info("Downloaded {} files from S3 and wrapped them in a zip", files.size());
    return zipBytes;
  }
  @Override
  public Map<String, String> getBucketPaths() {
    Map<String, String> bucketPaths = new HashMap<>();
    for (BucketPath bucketPath : BucketPath.values()) {
      bucketPaths.put(bucketPath.name().toLowerCase(), bucketPath.getPath());
    }
    return bucketPaths;
  }

  @Override
  public PresignUploadResponseDTO initiateMultipartUpload(StartUploadRequest request) {
    PresignUploadResponseDTO result = new PresignUploadResponseDTO();
    String fileKey = getFileKey(request.getUniqueId(), request.getFileName(), request.isOperationPageUpload());
    final long expiryMinutes = 10;
    result.setFileKey(fileKey);
    Map<Integer, URL> urls = new HashMap<>();
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .contentType(request.getFileType())
        .key(fileKey)
        .build();
    PresignedPutObjectRequest presignedPutObjectRequest
        = s3Presigner.presignPutObject(r -> r
        .putObjectRequest(putObjectRequest)
        .signatureDuration(Duration.ofMinutes(expiryMinutes)));
    urls.put(1, presignedPutObjectRequest.url());
    result.setUrls(urls);
    return result;
  }

  @Override
  public List<ResourceJsonUploadResponseDTO> initiateResourceUpload(ResourceJsonUploadDTO request) {
    List<ResourceJsonUploadResponseDTO> response = new ArrayList<>();
    for(var fileName: request.getResourceFileNames()) {
      String fileKey = "resources/" + request.getLanguageCode() + "/" + fileName;
      final long expiryMinutes = 10;
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
              .bucket(bucketName)
              .contentType("application/json")
              .key(fileKey)
              .build();
      PresignedPutObjectRequest presignedPutObjectRequest
              = s3Presigner.presignPutObject(r -> r
              .putObjectRequest(putObjectRequest)
              .signatureDuration(Duration.ofMinutes(expiryMinutes)));
      var url = new ResourceJsonUploadResponseDTO();
      url.setFileName(fileName);
      url.setUrl(presignedPutObjectRequest.url());
      response.add(url);
    }
    return response;
  }

  @Override
  public List<ResourceJsonUploadResponseDTO> getResourceFileDownloadUrls(ResourceJsonUploadDTO request) {
    List<ResourceJsonUploadResponseDTO> response = new ArrayList<>();
    for(var fileName: request.getResourceFileNames()) {
      String fileKey = "resources/" + request.getLanguageCode() + "/" + fileName;
      final long expiryMinutes = 10;
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
              .bucket(bucketName)
              .key(fileKey)
              .build();
      PresignedGetObjectRequest presignedGetObjectRequest
              = s3Presigner.presignGetObject(r -> r
              .getObjectRequest(getObjectRequest)
              .signatureDuration(Duration.ofMinutes(expiryMinutes)));
      var url = new ResourceJsonUploadResponseDTO();
      url.setFileName(fileName);
      url.setUrl(presignedGetObjectRequest.url());
      response.add(url);
    }
    return response;
  }

  private static String getFileKey(String uniqueId, String fileName, boolean isOperationPageUpload) {
    String tenantCode = TenantUtil.getTenantCode();
    if (isOperationPageUpload) {
      log.info("Generating file key for operation page upload fileName{}, unique id{}", fileName, uniqueId);
      return uniqueId + "/" + fileName;
    } else {
      return tenantCode + "/" + uniqueId + "/" + fileName;
    }
  }


}

