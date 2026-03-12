package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePhotoUploadResponse {
    private String photoUrl;
    private String message;
    private boolean success;
}
