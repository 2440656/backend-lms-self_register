package com.cognizant.lms.userservice.service;


import com.cognizant.lms.userservice.dto.PresignUploadResponseDTO;
import com.cognizant.lms.userservice.dto.ResourceJsonUploadDTO;
import com.cognizant.lms.userservice.dto.ResourceJsonUploadResponseDTO;
import com.cognizant.lms.userservice.dto.StartUploadRequest;
import com.cognizant.lms.userservice.dto.UploadFileRequest;


import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface LMSUploadService {

  String uploadFile(UploadFileRequest request);

  byte[] downloadZipFileFromS3(String uniqueId) throws IOException;

  Map<String, String> getBucketPaths() throws Exception;

  PresignUploadResponseDTO initiateMultipartUpload(StartUploadRequest request);

  List<ResourceJsonUploadResponseDTO> initiateResourceUpload(ResourceJsonUploadDTO request);

  List<ResourceJsonUploadResponseDTO> getResourceFileDownloadUrls(ResourceJsonUploadDTO request);
}
