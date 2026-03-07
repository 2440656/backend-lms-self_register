package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.ResourceJsonUploadDTO;
import com.cognizant.lms.userservice.dto.StartUploadRequest;
import com.cognizant.lms.userservice.dto.UploadFileRequest;
import com.cognizant.lms.userservice.service.LMSUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@Validated
public class LMSUploadController {

  private static final Logger log = LoggerFactory.getLogger(LMSUploadController.class);

  @Autowired
  private LMSUploadService uploadService;


  @PostMapping(path = "/uploadFile", consumes = {"multipart/form-data"})
  public ResponseEntity<HttpResponse> uploadFile(@RequestPart(value = "file") MultipartFile file, @RequestPart(value = "fileName") String fileName, @RequestPart(value = "uniqueId") String uniqueId) {
    log.info("Checking File " + file.getOriginalFilename() + " with fileName " + fileName + " and uniqueId " + uniqueId);
    HttpResponse httpResponse = new HttpResponse();
    try {
      UploadFileRequest request = new UploadFileRequest();
      request.setFile(file);
      request.setFileName(fileName);
      request.setUniqueId(uniqueId);
      var result = uploadService.uploadFile(request);
      httpResponse.setData(result);
      httpResponse.setStatus(HttpStatus.OK.value());
      log.info(String.valueOf(ResponseEntity.status(httpResponse.getStatus()).body(httpResponse)));
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    } catch (Exception exception) {
      httpResponse.setData(null);
      httpResponse.setError(exception.getMessage());
      httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    }
  }

  @GetMapping("/downloadAllFiles")
  public ResponseEntity<byte[]> downloadAllFiles(@RequestParam String uniqueId) {
    log.info(uniqueId);
    try {
      byte[] zipBytes = uploadService.downloadZipFileFromS3(uniqueId);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.TEXT_PLAIN);
      headers.setContentDispositionFormData("attachment", "Data.txt");
      String base64Zip = Base64.getEncoder().encodeToString(zipBytes);
      return new ResponseEntity<>(base64Zip.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    } catch (Exception exception) {
      log.info("Error downloading files: {}", exception.getMessage());
      HttpResponse httpResponse = new HttpResponse();
      httpResponse.setData(null);
      httpResponse.setError(exception.getMessage());
      httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(null);
    }
  }

  @GetMapping("/paths")
  public ResponseEntity<Map<String, Object>> getBucketPaths() {
    try {
      Map<String, String> paths = uploadService.getBucketPaths();
      Map<String, Object> response = Map.of("paths", paths);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
  @PostMapping("/initiate-upload")
  public ResponseEntity<HttpResponse> initiateMultipartUpload(
      @RequestBody StartUploadRequest request) {
    HttpResponse httpResponse = new HttpResponse();
    try {
      log.info("Initiating multipart upload for file: {}", request.getFileName());
      var result = uploadService.initiateMultipartUpload(request);
      httpResponse.setData(result);
      httpResponse.setStatus(HttpStatus.OK.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    } catch (Exception exception) {
      httpResponse.setData(null);
      httpResponse.setError(exception.getMessage());
      httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    }
  }

  @PostMapping("/initiate-resource-upload")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> initiateResourceUpload(
          @RequestBody ResourceJsonUploadDTO request) {
    HttpResponse httpResponse = new HttpResponse();
    try {
      log.info("Initiating resource upload for language: {}", request.getLanguageCode());
      var result = uploadService.initiateResourceUpload(request);
      httpResponse.setData(result);
      httpResponse.setStatus(HttpStatus.OK.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    } catch (Exception exception) {
      httpResponse.setData(null);
      httpResponse.setError(exception.getMessage());
      httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
    }
  }

@PostMapping("/get-resource-file-download-urls")
@PreAuthorize("hasAnyRole('system-admin','super-admin', 'content-admin', 'learner')")
public ResponseEntity<HttpResponse> getResourceFileDownloadUrls(
        @RequestBody ResourceJsonUploadDTO request) {
  HttpResponse httpResponse = new HttpResponse();
  try {
    log.info("Getting the resource download URLs: {}", request.getLanguageCode());
    var result = uploadService.getResourceFileDownloadUrls(request);
    httpResponse.setData(result);
    httpResponse.setStatus(HttpStatus.OK.value());
    return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
  } catch (Exception exception) {
    httpResponse.setData(null);
    httpResponse.setError(exception.getMessage());
    httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
  }
}
}