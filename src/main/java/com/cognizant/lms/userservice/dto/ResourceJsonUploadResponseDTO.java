package com.cognizant.lms.userservice.dto;

import lombok.Data;

import java.net.URL;

@Data
public class ResourceJsonUploadResponseDTO {
    private URL url;
    private String fileName;
}
