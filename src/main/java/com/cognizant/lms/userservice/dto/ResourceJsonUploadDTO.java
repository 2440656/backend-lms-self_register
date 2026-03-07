package com.cognizant.lms.userservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResourceJsonUploadDTO {
    private String languageCode;
    private List<String> resourceFileNames;
}
