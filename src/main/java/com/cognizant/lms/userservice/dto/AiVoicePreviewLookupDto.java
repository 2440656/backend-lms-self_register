package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiVoicePreviewLookupDto {
    private String pk;
    private String sk;
    private String fileName;
    private String gender;
    private String language;
    private String path;
    private String voiceId;
    private String voiceName;
    private String preSignedUrl;
}
