package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartUploadRequest {
  private String fileName;
  private String uniqueId;
  private String fileType;
  private Integer totalChunks;
  private boolean uploadInParts;
  private Map<String, String> metadata;
  private boolean operationPageUpload;
}
