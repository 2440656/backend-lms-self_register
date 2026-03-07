package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.UploadFileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cognizant.lms.userservice.config.S3config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LMSUploadServiceImplTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private S3Client s3Client;

  @Mock
  private S3config s3config;

  @InjectMocks
  private LMSUploadServiceImpl lmsUploadService;

  @InjectMocks
  private LMSUploadServiceImpl lmsUploadServiceLocal;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    String bucketName = "test-bucket";
    when(s3config.s3Client()).thenReturn(s3Client);
    objectMapper = mock(ObjectMapper.class);
    lmsUploadService = new LMSUploadServiceImpl(bucketName, s3config);
    lmsUploadServiceLocal = new LMSUploadServiceImpl(bucketName, s3config);
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[0]);
    doReturn(responseBytes).when(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
  }

  @Test
  public void testUploadFileToS3() throws Exception {
    UploadFileRequest request = new UploadFileRequest();
    request.setUniqueId("uniqueId");
    request.setFileName("fileName");
    request.setFile(new MockMultipartFile("file", "content".getBytes(StandardCharsets.UTF_8)));

    String expectedFileKey = getFileKey(request.getUniqueId(), request.getFileName());

    String result = lmsUploadService.uploadFile(request);

    assertNotNull(result);
    assertEquals(expectedFileKey, result);
    verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  private static String getFileKey(String uniqueId, String fileName) {
    String extension = getFileExtension(fileName);
    return switch (extension) {
      case "jpg", "jpeg", "png" -> Constants.IMAGES_PATH_PREFIX_S3
          + Constants.S3_CONTENT_PREFIX + uniqueId + "/" + fileName;
      default -> {
        if (fileName.contains("EmailTemplate")) {
          yield uniqueId + "/" + fileName;
        } else {
          yield uniqueId + "/" + fileName;
        }
      }
    };
  }

  private static String getFileExtension(String fileName) {
    int lastIndexOfDot = fileName.lastIndexOf(Constants.DOT);
    if (lastIndexOfDot == -1) {
      return Constants.EMPTY_STRING;
    }
    return fileName.substring(lastIndexOfDot + 1).toLowerCase();
  }

  @Test
  public void testDownloadZipFileFromS3() throws IOException {
    S3Object s3Object = S3Object.builder().key("tenantCode/templates/file1.txt").build();
    List<S3Object> s3Objects = Collections.singletonList(s3Object);

    ListObjectsV2Response listObjectsResponse = ListObjectsV2Response.builder()
        .contents(s3Objects)
        .build();
    doReturn(listObjectsResponse).when(s3Client).listObjectsV2(any(ListObjectsV2Request.class));

    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "file content".getBytes(StandardCharsets.UTF_8));
    doReturn(responseBytes).when(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

    String uniqueId = "uniqueId";
    byte[] zipBytes = lmsUploadService.downloadZipFileFromS3(uniqueId);

    assertNotNull(zipBytes);
    assertTrue(zipBytes.length > 0);

    verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
  }
@Test
  public void testGetBucketPaths() {
    Map<String, String> bucketPaths = lmsUploadService.getBucketPaths();
    assertNotNull(bucketPaths);
    assertEquals(3, bucketPaths.size());
    assertEquals("/images", bucketPaths.get("images"));
    assertEquals("/templates", bucketPaths.get("templates"));
    assertEquals("/uploads", bucketPaths.get("uploads"));
  }
}

