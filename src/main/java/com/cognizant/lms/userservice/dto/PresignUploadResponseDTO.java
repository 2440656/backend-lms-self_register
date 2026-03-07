package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignUploadResponseDTO {
  private String uploadId;

  private Map<Integer, URL> urls;

  private String fileKey;

  private String fileUploadId;
}
