package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UploadFileRequest;
import com.cognizant.lms.userservice.service.LMSUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LMSUploadControllerTest {

  @Mock
  private LMSUploadService uploadService;

  @InjectMocks
  private LMSUploadController uploadController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }


  @Test
  void uploadFile_ShouldReturnOkResponse() {
    MultipartFile file = mock(MultipartFile.class);
    String fileName = "testFileName";
    String uniqueId = "testUniqueId";
    HttpResponse expectedResponse = new HttpResponse();
    expectedResponse.setData("result");
    expectedResponse.setStatus(HttpStatus.OK.value());

    UploadFileRequest request = new UploadFileRequest();
    request.setFile(file);
    request.setFileName(fileName);
    request.setUniqueId(uniqueId);

    when(uploadService.uploadFile(request)).thenReturn("result");

    ResponseEntity<HttpResponse> response = uploadController.uploadFile(file, fileName, uniqueId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void uploadFile_Exception() {
    MultipartFile file = mock(MultipartFile.class);
    String fileName = "testFileName";
    String uniqueId = "testUniqueId";
    HttpResponse expectedResponse = new HttpResponse();
    expectedResponse.setData("result");
    expectedResponse.setStatus(HttpStatus.OK.value());

    UploadFileRequest request = new UploadFileRequest();
    request.setFile(file);
    request.setFileName(fileName);
    request.setUniqueId(uniqueId);

    when(uploadService.uploadFile(request)).thenThrow(new RuntimeException());

    ResponseEntity<HttpResponse> response = uploadController.uploadFile(file, fileName, uniqueId);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
  @Test
  void downloadAllFiles_ShouldReturnOkResponse() throws IOException {
    String uniqueId = "testUniqueId";
    byte[] zipBytes = "testData".getBytes(StandardCharsets.UTF_8);

    when(uploadService.downloadZipFileFromS3(uniqueId)).thenReturn(zipBytes);

    ResponseEntity<byte[]> response = uploadController.downloadAllFiles(uniqueId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    assertEquals("form-data; name=\"attachment\"; filename=\"Data.txt\"", response.getHeaders().getContentDisposition().toString());
    assertEquals(Base64.getEncoder().encodeToString(zipBytes), new String(response.getBody(), StandardCharsets.UTF_8));
  }

  @Test
  void downloadAllFiles_ShouldReturnBadRequestOnException() throws IOException {
    String uniqueId = "testUniqueId";

    when(uploadService.downloadZipFileFromS3(uniqueId)).thenThrow(new RuntimeException("Error occurred"));

    ResponseEntity<byte[]> response = uploadController.downloadAllFiles(uniqueId);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void getBucketPaths_ShouldReturnInternalServerErrorOnException() throws Exception {
    when(uploadService.getBucketPaths()).thenThrow(new RuntimeException("Error occurred"));

    ResponseEntity<Map<String, Object>> response = uploadController.getBucketPaths();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNull(response.getBody());
  }
}